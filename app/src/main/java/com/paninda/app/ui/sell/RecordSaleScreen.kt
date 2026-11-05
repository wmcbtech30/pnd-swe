package com.paninda.app.ui.sell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.ShopViewModel
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.Palette
import kotlinx.coroutines.launch
import java.util.UUID

private val LinesSaver = listSaver<List<SaleLineDraft>, String>(
    save = { it.flatMap { line -> listOf(line.productId, line.quantity, line.price) } },
    restore = { it.chunked(3).map { row -> SaleLineDraft(row[0], row[1], row[2]) } })

@Composable
fun RecordSaleScreen(state: ShopState, vm: ShopViewModel, initialProduct: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val id = rememberSaveable { UUID.randomUUID().toString() }
    var lines by rememberSaveable(stateSaver = LinesSaver) { mutableStateOf(state.products.find { it.id == initialProduct }
        ?.let { listOf(SaleLineDraft(it.id, price = Money.editable(it.unitPriceMinor))) }.orEmpty()) }
    val initialRecorder = rememberSaveable { state.accountEmail.orEmpty() }
    var recorder by rememberSaveable { mutableStateOf(initialRecorder) }
    var pick by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val total = runCatching { lines.fold(0L) { sum, line ->
        val quantity = requireNotNull(line.quantity.toIntOrNull()?.takeIf { it in 1..1_000_000 })
        Math.addExact(sum, Math.multiplyExact(quantity.toLong(), requireNotNull(Money.parse(line.price))))
    } }.getOrNull()
    val back = { if (lines.isNotEmpty() || recorder != initialRecorder) discard = true else onBack() }
    BackHandler { if (!busy) back() }
    PageScaffold(stringResource(R.string.sale_record_title), { if (!busy) back() },
        fab = { ActionFab(stringResource(if (busy) R.string.common_saving else R.string.sale_record_review), Icons.Default.Check, !busy) {
            if (lines.isEmpty() || total == null || recorder.isBlank()) error = R.string.recording_invalid else { error = 0; confirm = true }
        } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().testTag("sale-editor-list"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.sale_record_note), style = MaterialTheme.typography.bodySmall) }
            if (error != 0) item { Text(stringResource(error), color = Palette.Ink) }
            item { OutlinedTextField(recorder, { recorder = it.take(120) }, enabled = !busy,
                label = { Text(stringResource(R.string.sale_record_by)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item { OutlinedButton({ pick = true }, enabled = !busy) { Text(stringResource(R.string.sale_record_add)) } }
            items(lines, key = { it.productId }) { line ->
                val product = state.products.find { it.id == line.productId }
                Surface(color = Palette.Card, shape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row {
                            Text(product?.name ?: stringResource(R.string.recording_unavailable), Modifier.weight(1f))
                            IconButton({ lines = lines.filterNot { it.productId == line.productId } }, enabled = !busy) {
                                Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.sale_record_remove, product?.name.orEmpty()))
                            }
                        }
                        Text(stringResource(R.string.sale_record_available, product?.quantity ?: 0), style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(line.quantity, { value -> lines = lines.map { if (it.productId == line.productId) it.copy(quantity = value.take(7)) else it } },
                            enabled = !busy, label = { Text(stringResource(R.string.sale_record_quantity)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(line.price, { value -> lines = lines.map { if (it.productId == line.productId) it.copy(price = value.take(12)) else it } },
                            enabled = !busy, label = { Text(stringResource(R.string.sale_record_price)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item { DetailField(stringResource(R.string.sale_record_total), total?.let { stringResource(R.string.common_amount, Money.amount(it)) } ?: "—") }
        }
    }
    if (pick) SaleProductSheet(state.products.filter { it.isActive && it.quantity > 0 && lines.none { line -> line.productId == it.id } },
        { pick = false }) { product -> lines = lines + SaleLineDraft(product.id, price = Money.editable(product.unitPriceMinor)); pick = false }
    if (discard) DiscardRecordingDialog({ discard = false }, { discard = false; onBack() })
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, containerColor = Palette.White,
        title = { Text(stringResource(R.string.sale_record_confirm_title)) },
        text = { Text(stringResource(R.string.sale_record_confirm_body, Money.amount(total ?: 0))) },
        confirmButton = { TextButton({ confirm = false; busy = true
            scope.launch {
                val result = vm.recordSale(SaleDraft(id, recorder, lines))
                if (result is RecordResult.Saved) onSaved(result.id) else error = recordingError(result)
                busy = false
            }
        }) { Text(stringResource(R.string.sale_record_save)) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.common_cancel)) } })
}

package com.paninda.app.ui.receiving

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

private val ReceivingLinesSaver = listSaver<List<ReceivingLineDraft>, String>(
    save = { it.flatMap { line -> listOf(line.productId, line.quantity) } },
    restore = { it.chunked(2).map { row -> ReceivingLineDraft(row[0], row[1]) } })

@Composable
fun NewReceivingScreen(state: ShopState, vm: ShopViewModel, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val today = rememberShopDate()
    val initialDate = rememberSaveable { today.toString() }
    val initialRecorder = rememberSaveable { state.accountEmail.orEmpty() }
    val id = rememberSaveable { UUID.randomUUID().toString() }
    var vendorId by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(initialDate) }
    var recorder by rememberSaveable { mutableStateOf(initialRecorder) }
    var reference by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var lines by rememberSaveable(stateSaver = ReceivingLinesSaver) { mutableStateOf(emptyList<ReceivingLineDraft>()) }
    var picker by rememberSaveable { mutableStateOf("") }
    var nextVendor by rememberSaveable { mutableStateOf<String?>(null) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberLazyListState()
    LaunchedEffect(error) { if (error != 0) scroll.animateScrollToItem(0) }
    val vendor = state.vendors.find { it.id == vendorId }
    val units = lines.sumOf { it.quantity.toLongOrNull() ?: 0L }
    val back = { if (vendorId.isNotEmpty() || lines.isNotEmpty() || reference.isNotBlank() || notes.isNotBlank() || date != initialDate || recorder != initialRecorder) discard = true else onBack() }
    BackHandler { if (!busy) back() }
    PageScaffold(stringResource(R.string.receiving_editor_title), { if (!busy) back() },
        fab = { ActionFab(stringResource(if (busy) R.string.common_saving else R.string.receiving_editor_review), Icons.Default.Check, !busy) {
            val parsed = receivingDate(date)
            error = if (vendor?.isActive != true || parsed == null || parsed > today || recorder.isBlank() ||
                lines.isEmpty() || lines.size > 1000 || lines.any { it.quantity.toIntOrNull() !in 1..1_000_000 }) R.string.receiving_editor_invalid else 0
            if (error == 0) confirm = true
        } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().testTag("receiving-editor-list"), state = scroll,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.receiving_editor_note), style = MaterialTheme.typography.bodySmall) }
            if (error != 0) item { Text(stringResource(error)) }
            item { OutlinedButton({ picker = "vendor" }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(vendor?.name ?: stringResource(R.string.receiving_editor_vendor))
            } }
            item { ReceivingField(date, { date = it.take(10) }, R.string.receiving_editor_date, !busy) }
            item { ReceivingField(recorder, { recorder = it.take(120) }, R.string.receiving_editor_by, !busy) }
            item { ReceivingField(reference, { reference = it.take(120) }, R.string.receiving_editor_reference, !busy) }
            item { ReceivingField(notes, { notes = it.take(1000) }, R.string.receiving_editor_notes, !busy, multiline = true) }
            item { OutlinedButton({ picker = "product" }, enabled = !busy && vendor?.isActive == true) { Text(stringResource(R.string.receiving_editor_add)) } }
            if (state.products.none { it.isActive } || state.vendors.none { it.isActive }) item { Text(stringResource(R.string.receiving_editor_setup), style = MaterialTheme.typography.bodySmall) }
            items(lines, key = { it.productId }) { line ->
                val product = state.products.find { it.id == line.productId }
                Surface(color = Palette.Card, shape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row {
                            Text(product?.name ?: stringResource(R.string.receiving_editor_changed), Modifier.weight(1f))
                            IconButton({ lines = lines.filterNot { it.productId == line.productId } }, enabled = !busy) {
                                Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.receiving_editor_remove, product?.name.orEmpty()))
                            }
                        }
                        Text(stringResource(R.string.receiving_editor_product, product?.sku.orEmpty(), product?.quantity ?: 0), style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(line.quantity, { value -> lines = lines.map { if (it.productId == line.productId) it.copy(quantity = value.take(7)) else it } },
                            enabled = !busy, label = { Text(stringResource(R.string.receiving_editor_quantity)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item { Text(stringResource(R.string.receiving_editor_total, lines.size, units)) }
        }
    }
    if (picker == "vendor") ReceivingPickerSheet(stringResource(R.string.receiving_editor_vendor),
        state.vendors.filter { it.isActive }.map { ReceivingChoice(it.id, it.name) }, { picker = "" }) { selected ->
        picker = ""
        if (selected != vendorId && lines.isNotEmpty()) nextVendor = selected else vendorId = selected
    }
    if (picker == "product") ReceivingPickerSheet(stringResource(R.string.receiving_editor_add),
        state.products.filter { it.isActive && it.vendorId == vendorId && lines.none { line -> line.productId == it.id } }
            .map { ReceivingChoice(it.id, it.name, it.sku) }, { picker = "" }) { selected ->
        if (lines.none { it.productId == selected }) lines = lines + ReceivingLineDraft(selected)
        picker = ""
    }
    if (nextVendor != null) AlertDialog(onDismissRequest = { nextVendor = null }, containerColor = Palette.White,
        title = { Text(stringResource(R.string.receiving_editor_change_vendor)) },
        text = { Text(stringResource(R.string.receiving_editor_change_body)) },
        confirmButton = { TextButton({ vendorId = nextVendor.orEmpty(); lines = emptyList(); nextVendor = null }) { Text(stringResource(R.string.common_apply)) } },
        dismissButton = { TextButton({ nextVendor = null }) { Text(stringResource(R.string.common_cancel)) } })
    if (discard) DiscardRecordingDialog({ discard = false }, { discard = false; onBack() })
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, containerColor = Palette.White,
        title = { Text(stringResource(R.string.receiving_editor_confirm_title)) },
        text = { Text(stringResource(R.string.receiving_editor_confirm_body, vendor?.name.orEmpty(), date.trim(), lines.size, units),
            Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton({ confirm = false; busy = true
            scope.launch {
                val result = vm.recordReceiving(ReceivingDraft(id, vendorId, date, recorder, reference, notes, lines))
                if (result is RecordResult.Saved) onSaved(result.id)
                else error = when (result) {
                    RecordResult.Invalid -> R.string.receiving_editor_invalid
                    RecordResult.Unavailable -> R.string.receiving_editor_changed
                    else -> R.string.common_action_failed
                }
                busy = false
            }
        }) { Text(stringResource(R.string.receiving_editor_save)) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.common_cancel)) } })
}

@Composable
private fun ReceivingField(value: String, onChange: (String) -> Unit, label: Int, enabled: Boolean, multiline: Boolean = false) {
    OutlinedTextField(value, onChange, enabled = enabled, label = { Text(stringResource(label), style = MaterialTheme.typography.bodySmall) },
        singleLine = !multiline, minLines = if (multiline) 2 else 1, modifier = Modifier.fillMaxWidth())
}

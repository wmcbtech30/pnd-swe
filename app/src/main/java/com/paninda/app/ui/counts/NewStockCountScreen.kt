package com.paninda.app.ui.counts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

private val EntriesSaver = listSaver<List<CountEntryDraft>, String>(
    save = { it.flatMap { entry -> listOf(entry.productId, entry.expectedQuantity.toString(), entry.expectedVersion.toString(), entry.countedQuantity) } },
    restore = { it.chunked(4).map { row -> CountEntryDraft(row[0], row[1].toInt(), row[2].toLong(), row[3]) } })

@Composable
fun NewStockCountScreen(state: ShopState, vm: ShopViewModel, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val id = rememberSaveable { UUID.randomUUID().toString() }
    var entries by rememberSaveable(stateSaver = EntriesSaver) { mutableStateOf(state.products.filter { it.isActive }
        .map { CountEntryDraft(it.id, it.quantity, it.version) }) }
    val initialRecorder = rememberSaveable { state.accountEmail.orEmpty() }
    var recorder by rememberSaveable { mutableStateOf(initialRecorder) }
    var notes by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val entered = entries.filter { it.countedQuantity.isNotBlank() }
    val variances = entered.count { it.countedQuantity.toIntOrNull()?.toLong() != it.expectedQuantity.toLong() }
    val back = { if (entered.isNotEmpty() || notes.isNotBlank() || recorder != initialRecorder) discard = true else onBack() }
    BackHandler { if (!busy) back() }
    PageScaffold(stringResource(R.string.count_editor_title), { if (!busy) back() },
        header = { SearchField(query, { query = it }, stringResource(R.string.count_editor_search)) },
        fab = { ActionFab(stringResource(if (busy) R.string.common_saving else R.string.count_editor_review), Icons.Default.Check, !busy && entries.isNotEmpty()) {
            error = if (recorder.isBlank() || entered.isEmpty() || entered.any { it.countedQuantity.toIntOrNull() !in 0..1_000_000 } ||
                (variances > 0 && notes.isBlank())) R.string.count_editor_invalid else 0
            if (error == 0) confirm = true
        } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().testTag("count-editor-list"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.count_editor_note), style = MaterialTheme.typography.bodySmall) }
            if (error != 0) item { Text(stringResource(error)) }
            item { OutlinedTextField(recorder, { recorder = it.take(120) }, enabled = !busy,
                label = { Text(stringResource(R.string.count_editor_by)) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(notes, { notes = it.take(1000) }, enabled = !busy,
                label = { Text(stringResource(R.string.count_editor_notes)) }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
            if (entries.isEmpty()) item { EmptyState(stringResource(R.string.count_editor_empty)) }
            val rows = entries.filter { entry -> state.products.find { it.id == entry.productId }?.let {
                query.isBlank() || it.name.contains(query, true) || it.sku.contains(query, true)
            } == true }
            if (entries.isNotEmpty() && rows.isEmpty()) item { Text(stringResource(R.string.common_no_matches)) }
            items(rows, key = { it.productId }) { entry ->
                val product = state.products.find { it.id == entry.productId }
                val counted = entry.countedQuantity.toIntOrNull()
                Surface(color = if (counted != null && counted != entry.expectedQuantity) Palette.ProductWarning else Palette.Card,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(product?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                        Text(product?.sku.orEmpty(), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.count_editor_expected, entry.expectedQuantity))
                        OutlinedTextField(entry.countedQuantity, { value -> entries = entries.map { if (it.productId == entry.productId) it.copy(countedQuantity = value.take(7)) else it } },
                            enabled = !busy, label = { Text(stringResource(R.string.count_editor_physical)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().testTag("count-" + entry.productId))
                        if (counted != null) Text(stringResource(R.string.count_editor_variance, signedCount(counted.toLong() - entry.expectedQuantity)))
                    }
                }
            }
        }
    }
    if (discard) DiscardRecordingDialog({ discard = false }, { discard = false; onBack() })
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, containerColor = Palette.White,
        title = { Text(stringResource(R.string.count_editor_confirm_title)) },
        text = { Text(stringResource(R.string.count_editor_confirm_body, entered.size, variances)) },
        confirmButton = { TextButton({ confirm = false; busy = true
            scope.launch {
                val result = vm.recordCount(CountDraft(id, recorder, notes, entries))
                if (result is RecordResult.Saved) onSaved(result.id) else error = recordingError(result)
                busy = false
            }
        }) { Text(stringResource(R.string.count_editor_confirm)) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.common_cancel)) } })
}

internal fun signedCount(value: Long) = if (value > 0) "+$value" else value.toString()

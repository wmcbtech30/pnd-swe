package com.paninda.app.ui.receiving

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.ui.components.SearchField
import com.paninda.app.ui.theme.Palette

internal data class ReceivingChoice(val id: String, val name: String, val detail: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceivingPickerSheet(title: String, choices: List<ReceivingChoice>, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.White) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.8f).navigationBarsPadding()) {
            Text(title, Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
            SearchField(query, { query = it }, stringResource(R.string.receiving_picker_search))
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp)) {
                val rows = choices.filter { it.name.contains(query.trim(), true) || it.detail.contains(query.trim(), true) }
                if (rows.isEmpty()) item { Text(stringResource(R.string.receiving_picker_empty)) }
                items(rows, key = { it.id }) { choice -> TextButton({ onSelect(choice.id) }, Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(choice.name)
                        if (choice.detail.isNotBlank()) Text(choice.detail, style = MaterialTheme.typography.bodySmall)
                    }
                } }
            }
        }
    }
}

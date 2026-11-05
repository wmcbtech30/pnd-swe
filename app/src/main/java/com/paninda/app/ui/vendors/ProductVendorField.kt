package com.paninda.app.ui.vendors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.Vendor
import com.paninda.app.ui.components.SearchField
import com.paninda.app.ui.theme.Palette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductVendorField(value: String, onText: (String) -> Unit, selectedId: String?, vendors: List<Vendor>,
    enabled: Boolean, onSelect: (Vendor) -> Unit) {
    var show by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    Column {
        OutlinedTextField(value, onText, label = { Text(stringResource(R.string.product_editor_vendor), style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled,
            trailingIcon = { IconButton({ show = true }, enabled = enabled) {
                Icon(Icons.Default.ArrowDropDown, stringResource(R.string.vendor_picker_title))
            } })
        Text(stringResource(if (vendors.any { it.id == selectedId && !it.isActive }) R.string.vendor_picker_archived
            else R.string.vendor_picker_hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
    }
    if (show) ModalBottomSheet(onDismissRequest = { show = false }, containerColor = Palette.White) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.75f).padding(bottom = 20.dp)) {
            Text(stringResource(R.string.vendor_picker_title), Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.titleLarge)
            SearchField(query, { query = it }, stringResource(R.string.vendors_search))
            val rows = vendors.filter { it.isActive && (query.isBlank() || it.name.contains(query, true)) }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                if (rows.isEmpty()) item { Text(stringResource(R.string.vendor_picker_empty)) }
                items(rows, key = { it.id }) { vendor ->
                    Row(Modifier.fillMaxWidth().clickable { onSelect(vendor); show = false; query = "" }
                        .padding(vertical = 14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(vendor.id == selectedId, { onSelect(vendor); show = false; query = "" })
                        Text(vendor.name, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

package com.paninda.app.ui.vendors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.Palette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorsScreen(vendors: List<Vendor>, products: List<Product>, onBack: () -> Unit,
    onNew: () -> Unit, onVendor: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(VendorFilter.ACTIVE) }
    var showFilter by rememberSaveable { mutableStateOf(false) }
    val rows = vendors.filter { filter.matches(it, query) }
    val scroll = rememberLazyListState()
    LaunchedEffect(query, filter) { scroll.scrollToItem(0) }
    PageScaffold(stringResource(R.string.vendors_title), onBack,
        actions = { FilterAction(filter != VendorFilter.ACTIVE) { showFilter = true } },
        header = { SearchField(query, { query = it }, stringResource(R.string.vendors_search)) },
        fab = { ActionFab(stringResource(R.string.vendors_new), Icons.Default.Add, onClick = onNew) }) { padding ->
        if (rows.isEmpty()) EmptyState(stringResource(if (vendors.isEmpty()) R.string.vendors_empty else R.string.common_no_matches), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding), state = scroll,
            contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(rows, key = { it.id }) { vendor ->
                val linked = products.filter { it.vendorId == vendor.id && it.isActive }
                Card(Modifier.fillMaxWidth().clickable { onVendor(vendor.id) },
                    colors = CardDefaults.cardColors(containerColor = Palette.Card)) {
                    Row(Modifier.padding(14.dp)) {
                        Icon(Icons.Outlined.Storefront, null, Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(vendor.name, style = MaterialTheme.typography.titleMedium)
                            if (!vendor.isActive) Text(stringResource(R.string.vendors_archived), style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.vendors_summary, linked.size, linked.sumOf { it.quantity.toLong() }),
                                style = MaterialTheme.typography.bodyMedium)
                            val contact = vendor.contactPerson.ifBlank { vendor.phone.ifBlank { vendor.email } }
                            if (contact.isNotBlank()) Text(contact, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    if (showFilter) ModalBottomSheet(onDismissRequest = { showFilter = false }, containerColor = Palette.White) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text(stringResource(R.string.vendors_filter), style = MaterialTheme.typography.titleLarge)
            VendorFilter.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { filter = option; showFilter = false }.padding(vertical = 10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(filter == option, onClick = { filter = option; showFilter = false })
                    Text(stringResource(when (option) {
                        VendorFilter.ACTIVE -> R.string.vendors_active
                        VendorFilter.ARCHIVED -> R.string.vendors_archived
                        VendorFilter.ALL -> R.string.vendors_all
                    }))
                }
            }
        }
    }
}

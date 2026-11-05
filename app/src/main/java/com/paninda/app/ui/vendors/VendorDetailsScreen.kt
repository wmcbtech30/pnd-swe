package com.paninda.app.ui.vendors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.ShopViewModel
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Composable
fun VendorDetailsScreen(id: String, state: ShopState, vm: ShopViewModel, onBack: () -> Unit,
    onEdit: () -> Unit, onNewProduct: () -> Unit, onProduct: (String) -> Unit) {
    val vendor = state.vendors.find { it.id == id }
    val products = state.products.filter { it.vendorId == id }
    var query by rememberSaveable(id) { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    PageScaffold(stringResource(R.string.vendor_details_title), onBack,
        actions = {
            if (vendor != null) {
                IconButton(onEdit, enabled = !busy) { Icon(Icons.Outlined.Edit, stringResource(R.string.vendor_details_edit)) }
                IconButton({ confirm = true }, enabled = !busy) {
                    Icon(if (vendor.isActive) Icons.Outlined.Archive else Icons.Outlined.Unarchive,
                        stringResource(if (vendor.isActive) R.string.vendor_details_archive else R.string.vendor_details_restore))
                }
            }
        }, header = { SearchField(query, { query = it }, stringResource(R.string.vendor_details_search)) },
        fab = { if (vendor?.isActive == true) ActionFab(stringResource(R.string.vendor_details_add_product), Icons.Default.Add, !busy, onNewProduct) }) { padding ->
        if (vendor == null) EmptyState(stringResource(R.string.vendor_details_missing), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (query.isBlank()) item {
                Text(vendor.name, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(if (vendor.isActive) R.string.vendors_active else R.string.vendors_archived), color = Palette.Section)
                if (!vendor.isActive) Text(stringResource(R.string.vendor_details_archived_note), style = MaterialTheme.typography.bodyMedium)
                if (failed) Text(stringResource(R.string.common_action_failed), Modifier.padding(vertical = 8.dp))
                val active = products.filter { it.isActive }
                DetailField(stringResource(R.string.vendor_details_stock), stringResource(R.string.vendors_summary,
                    active.size, active.sumOf { it.quantity.toLong() }))
                Text(stringResource(R.string.vendor_details_alerts, active.count { it.isLowStock }, active.count { it.isOutOfStock }),
                    style = MaterialTheme.typography.bodyMedium)
                val fields = listOf(R.string.vendor_editor_contact to vendor.contactPerson, R.string.vendor_editor_phone to vendor.phone,
                    R.string.vendor_editor_email to vendor.email, R.string.vendor_editor_address to vendor.address,
                    R.string.vendor_editor_notes to vendor.notes)
                fields.filter { it.second.isNotBlank() }.forEach { (label, value) -> DetailField(stringResource(label), value) }
                Spacer(Modifier.height(16.dp))
            }
            item { SectionLabel(stringResource(R.string.vendor_details_products)) }
            val rows = products.filter { query.isBlank() || it.name.contains(query, true) || it.sku.contains(query, true) }
            if (rows.isEmpty()) item { Text(stringResource(if (products.isEmpty()) R.string.vendor_details_empty else R.string.common_no_matches)) }
            items(rows, key = { it.id }) { product -> ProductCard(product) { onProduct(product.id) } }
        }
    }
    if (confirm && vendor != null) AlertDialog(onDismissRequest = { confirm = false }, containerColor = Palette.White,
        title = { Text(stringResource(if (vendor.isActive) R.string.vendor_details_archive_title else R.string.vendor_details_restore_title)) },
        text = { Text(stringResource(if (vendor.isActive) R.string.vendor_details_archive_body else R.string.vendor_details_restore_body)) },
        confirmButton = { TextButton({
            confirm = false; busy = true; failed = false
            scope.launch(Dispatchers.Main.immediate) {
                failed = vm.setVendorActive(id, !vendor.isActive) !is SaveVendorResult.Saved
                busy = false
            }
        }) { Text(stringResource(if (vendor.isActive) R.string.vendor_details_archive else R.string.vendor_details_restore)) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.common_cancel)) } })
}

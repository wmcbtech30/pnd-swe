package com.paninda.app.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.ShopViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paninda.app.ui.vendors.ProductVendorField
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Composable
fun ProductEditorScreen(productId: String?, products: List<Product>, vm: ShopViewModel, onBack: () -> Unit, initialVendorId: String? = null) {
    val product = products.find { it.id == productId }
    val shop by vm.state.collectAsStateWithLifecycle()
    val initialVendor = shop.vendors.find { it.id == (product?.vendorId ?: initialVendorId) }
    var vendorId by rememberSaveable(productId) { mutableStateOf(product?.vendorId ?: initialVendorId) }
    var name by rememberSaveable(productId) { mutableStateOf(product?.name.orEmpty()) }
    var sku by rememberSaveable(productId) { mutableStateOf(product?.sku.orEmpty()) }
    var vendor by rememberSaveable(productId) { mutableStateOf(initialVendor?.name ?: product?.vendorName.orEmpty()) }
    var category by rememberSaveable(productId) { mutableStateOf(product?.category.orEmpty()) }
    var price by rememberSaveable(productId) { mutableStateOf(product?.unitPriceMinor?.let(Money::editable).orEmpty()) }
    var quantity by rememberSaveable(productId) { mutableStateOf("0") }
    var threshold by rememberSaveable(productId) { mutableStateOf(product?.lowStockThreshold?.toString() ?: "0") }
    var error by rememberSaveable { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val original = remember(productId, initialVendorId) { ProductDraft(product?.name.orEmpty(), product?.sku.orEmpty(), initialVendor?.name ?: product?.vendorName.orEmpty(),
        product?.category.orEmpty(), product?.unitPriceMinor?.let(Money::editable).orEmpty(), "0",
        product?.lowStockThreshold?.toString() ?: "0", product?.vendorId ?: initialVendorId) }
    val draft = ProductDraft(name, sku, vendor, category, price, quantity, threshold, vendorId)
    val dirty = draft != original
    val back = { if (dirty) discard = true else onBack() }
    BackHandler(enabled = dirty && !saving) { discard = true }
    BackHandler(enabled = saving) { /* Wait for the local transaction to finish. */ }

    PageScaffold(stringResource(if (productId == null) R.string.product_editor_new else R.string.product_editor_edit),
        onBack = { if (!saving) back() },
        fab = {
            if (productId == null || product != null) ActionFab(
                stringResource(if (saving) R.string.common_saving else R.string.product_editor_save),
                Icons.Default.Check, enabled = !saving,
            ) {
                saving = true
                error = 0
                scope.launch(Dispatchers.Main.immediate) {
                    when (vm.saveProduct(productId, draft)) {
                        is SaveProductResult.Saved -> onBack()
                        SaveProductResult.Invalid -> error = R.string.product_editor_invalid
                        SaveProductResult.DuplicateSku -> error = R.string.product_editor_duplicate
                        SaveProductResult.VendorUnavailable -> error = R.string.vendor_picker_unavailable
                        SaveProductResult.Missing -> error = R.string.product_editor_missing
                        SaveProductResult.Failed -> error = R.string.common_action_failed
                    }
                    saving = false
                }
            }
        },
    ) { padding ->
        if (productId != null && product == null) EmptyState(stringResource(R.string.product_editor_missing), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().testTag("product-editor-list"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (error != 0) item {
                Surface(color = Palette.ProductWarning) {
                    Text(stringResource(error), modifier = Modifier.padding(12.dp), fontSize = 16.sp)
                }
            }
            item { EditorField(name, { name = it.take(120) }, R.string.product_editor_name, enabled = !saving) }
            item { EditorField(sku, { sku = it.take(64) }, R.string.product_editor_sku,
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), enabled = !saving) }
            item { ProductVendorField(vendor, { vendor = it.take(120); vendorId = null }, vendorId, shop.vendors, !saving) {
                vendor = it.name; vendorId = it.id
            } }
            item { EditorField(category, { category = it.take(80) }, R.string.product_editor_category, enabled = !saving) }
            item { EditorField(price, { price = it.take(10) }, R.string.product_editor_price,
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), enabled = !saving) }
            if (productId == null) item {
                EditorField(quantity, { quantity = it.take(7) }, R.string.product_editor_quantity,
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !saving)
            }
            item { EditorField(threshold, { threshold = it.take(7) }, R.string.product_editor_threshold,
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !saving) }
            if (productId != null) item { Text(stringResource(R.string.product_editor_stock_note),
                fontSize = 14.sp, color = Palette.Section) }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, containerColor = Palette.White,
        title = { Text(stringResource(R.string.product_editor_unsaved_title)) },
        text = { Text(stringResource(R.string.product_editor_unsaved_body)) },
        confirmButton = { TextButton({ discard = false; onBack() }) { Text(stringResource(R.string.product_editor_discard)) } },
        dismissButton = { TextButton({ discard = false }) { Text(stringResource(R.string.product_editor_keep)) } })
}

@Composable
private fun EditorField(value: String, onChange: (String) -> Unit, label: Int,
    keyboard: KeyboardOptions = KeyboardOptions.Default, enabled: Boolean = true) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label), fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = keyboard, enabled = enabled)
}

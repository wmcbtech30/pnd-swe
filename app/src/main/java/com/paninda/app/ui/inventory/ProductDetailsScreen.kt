package com.paninda.app.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.ShopViewModel
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.Palette
import kotlinx.coroutines.CancellationException

@Composable
fun ProductDetailsScreen(productId: String, products: List<Product>, vm: ShopViewModel,
    onBack: () -> Unit, onEdit: () -> Unit) {
    val product = products.find { it.id == productId }
    var failed by remember(productId) { mutableStateOf(false) }
    val movements by produceState(emptyList<StockMovement>(), productId) {
        try { value = vm.movements(productId) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    PageScaffold(stringResource(R.string.product_details_title), onBack = onBack,
        fab = { if (product != null) ActionFab(stringResource(R.string.product_details_edit),
            Icons.Default.Edit, onClick = onEdit) },
    ) { padding ->
        if (product == null) EmptyState(stringResource(R.string.product_details_missing), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 12.dp, bottom = 112.dp)) {
            item {
                Text(product.name, fontSize = 28.sp, lineHeight = 36.sp)
                DetailField(stringResource(R.string.product_details_sku), product.sku)
                DetailField(stringResource(R.string.product_details_vendor), product.vendorName)
                DetailField(stringResource(R.string.product_details_category), product.category)
                DetailField(stringResource(R.string.product_details_price),
                    stringResource(R.string.common_amount, Money.amount(product.unitPriceMinor)))
                DetailField(stringResource(R.string.product_details_stock), product.quantity.toString())
                DetailField(stringResource(R.string.product_details_threshold), product.lowStockThreshold.toString())
                Text(stringResource(R.string.product_details_pending), color = Palette.Section,
                    fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(vertical = 16.dp))
                SectionLabel(stringResource(R.string.product_details_movements))
            }
            if (failed || movements.isEmpty()) item {
                Text(stringResource(if (failed) R.string.common_load_failed else R.string.product_details_empty),
                    fontSize = 16.sp, modifier = Modifier.padding(vertical = 12.dp))
            }
            items(movements, key = { it.id }) { movement ->
                DetailField(stringResource(R.string.product_details_movement,
                    when (movement.movementType) {
                        "OPENING" -> stringResource(R.string.product_details_opening)
                        "SALE" -> stringResource(R.string.movement_reports_sale)
                        "COUNT_ADJUSTMENT" -> stringResource(R.string.movement_reports_count)
                        else -> movement.movementType
                    },
                    pluralStringResource(R.plurals.product_details_units, movement.quantityDelta, movement.quantityDelta)),
                    dateTimeLabel(movement.occurredAt))
            }
        }
    }
}

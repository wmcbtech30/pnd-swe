package com.paninda.app.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.*
import com.paninda.app.ui.filters.*
import com.paninda.app.ui.theme.*

@Composable
fun StockMovementReportScreen(state: ShopState, onBack: () -> Unit, onProduct: (String) -> Unit) {
    var dates by rememberDateFilter(DateRange.ALL)
    var vendorId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDates by rememberSaveable { mutableStateOf(false) }
    var showVendors by rememberSaveable { mutableStateOf(false) }
    val today = rememberShopDate()
    val report = remember(state.movements, state.products, dates, today, vendorId) {
        MovementReport.from(state.movements, state.products, dates, today, vendorId)
    }
    val products = remember(state.products) { state.products.associateBy { it.id } }
    val vendorName = state.vendors.find { it.id == vendorId }?.name
    val dateLabel = when (dates.range) {
        DateRange.ALL -> stringResource(R.string.filters_all_dates)
        DateRange.TODAY -> stringResource(R.string.filters_today)
        DateRange.WEEK -> stringResource(R.string.filters_week)
        DateRange.MONTH -> stringResource(R.string.filters_month)
        DateRange.CUSTOM -> stringResource(R.string.movement_reports_range, dates.start, dates.end)
    }
    PageScaffold(stringResource(R.string.reports_movements), onBack,
        subtitle = stringResource(R.string.movement_reports_scope),
        actions = {
            IconButton({ showDates = true }) { Icon(Icons.Outlined.CalendarMonth, stringResource(R.string.movement_reports_dates),
                tint = if (dates.range != DateRange.ALL) Palette.Blue else Palette.Icon) }
            IconButton({ showVendors = true }) { Icon(Icons.Outlined.FilterAlt, stringResource(R.string.movement_reports_vendor),
                tint = if (vendorId != null) Palette.Blue else Palette.Icon) }
        }, header = {
            Text(stringResource(R.string.movement_reports_filters, dateLabel, vendorName ?: stringResource(R.string.movement_reports_all_vendors)),
                Modifier.padding(horizontal = 26.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ReportCard(stringResource(R.string.movement_reports_net), signed(report.net), Modifier.fillMaxWidth(),
                note = pluralStringResource(R.plurals.movement_reports_count, report.rows.size, report.rows.size)) }
            item { MovementTotals(report.added, report.removed) }
            item { Text(stringResource(R.string.movement_reports_note), style = MaterialTheme.typography.bodySmall) }
            if (report.rows.isEmpty()) item { EmptyState(stringResource(R.string.movement_reports_empty)) }
            items(report.rows, key = { it.id }) { movement ->
                val product = products[movement.productId]
                Surface(onClick = { onProduct(movement.productId) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp),
                    color = if (movement.quantityDelta < 0) Palette.ProductWarning else Palette.Card) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(product?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.movement_reports_product, product?.sku.orEmpty(), product?.vendorName.orEmpty()), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.movement_reports_delta, movementTypeLabel(movement.movementType), signed(movement.quantityDelta.toLong())))
                        Text(movement.reason, style = MaterialTheme.typography.bodySmall)
                        Text(dateTimeLabel(movement.occurredAt), style = MaterialTheme.typography.bodySmall, color = Palette.Section)
                    }
                }
            }
        }
    }
    if (showDates) DateFilterSheet(dates, { showDates = false }) { dates = it; showDates = false }
    if (showVendors) MovementVendorSheet(vendorId, state, { showVendors = false }) { vendorId = it; showVendors = false }
}

internal fun signed(value: Long) = if (value > 0) "+$value" else value.toString()

@Composable
private fun movementTypeLabel(type: String): String = when (type) {
    "OPENING" -> stringResource(R.string.movement_reports_opening)
    "RECEIVING" -> stringResource(R.string.movement_reports_receiving)
    "ADJUSTMENT" -> stringResource(R.string.movement_reports_adjustment)
    "SALE" -> stringResource(R.string.movement_reports_sale)
    else -> type
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovementTotals(added: Long, removed: Long) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportCard(stringResource(R.string.movement_reports_added), added.toString(), Modifier.weight(1f).widthIn(min = 145.dp))
        ReportCard(stringResource(R.string.movement_reports_removed), removed.toString(), Modifier.weight(1f).widthIn(min = 145.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovementVendorSheet(selected: String?, state: ShopState, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.White) {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(24.dp)) {
            Text(stringResource(R.string.movement_reports_vendor), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.movement_reports_vendor_note), style = MaterialTheme.typography.bodySmall)
            TextButton({ onSelect(null) }) { Text(stringResource(R.string.movement_reports_all_vendors)) }
            state.vendors.forEach { vendor ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected == vendor.id, { onSelect(vendor.id) })
                    TextButton({ onSelect(vendor.id) }) { Text(vendor.name) }
                }
            }
        }
    }
}

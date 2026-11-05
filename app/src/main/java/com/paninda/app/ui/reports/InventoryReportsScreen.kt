package com.paninda.app.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.*
import com.paninda.app.ui.filters.*
import com.paninda.app.ui.theme.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryReportsScreen(state: ShopState, onBack: () -> Unit, onMovements: () -> Unit, onProduct: (String) -> Unit) {
    var filter by rememberProductFilter()
    var showFilter by rememberSaveable { mutableStateOf(false) }
    val report = remember(state.products, filter) { InventoryReport.from(state.products, filter) }
    PageScaffold(stringResource(R.string.reports_title), onBack,
        subtitle = stringResource(R.string.reports_current),
        actions = { IconButton({ showFilter = true }) {
            Icon(Icons.Outlined.FilterAlt, stringResource(R.string.reports_filter), tint = if (filter.active) Palette.Blue else Palette.Icon)
        } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(if (filter.active) R.string.reports_filtered else R.string.reports_all),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
                if (filter.active) TextButton({ filter = ProductFilter() }) { Text(stringResource(R.string.common_clear)) }
            }
            item { MenuRow(stringResource(R.string.reports_movements), Icons.Outlined.SwapVert, onClick = onMovements) }
            if (report.products.isEmpty()) item {
                EmptyState(stringResource(if (state.products.none { it.isActive }) R.string.reports_empty else R.string.common_no_matches))
            } else {
                item { ReportCard(stringResource(R.string.reports_units), report.units.toString(), Modifier.fillMaxWidth(),
                    note = pluralStringResource(R.plurals.reports_products_count, report.products.size, report.products.size)) }
                item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportCard(stringResource(R.string.reports_low), report.low.toString(), Modifier.weight(1f).widthIn(min = 145.dp), report.low > 0)
                    ReportCard(stringResource(R.string.reports_out), report.out.toString(), Modifier.weight(1f).widthIn(min = 145.dp), report.out > 0)
                } }
                item { ReportCard(stringResource(R.string.reports_value),
                    stringResource(R.string.common_amount, DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(BigDecimal(report.retailValueMinor, 2))),
                    Modifier.fillMaxWidth(), note = stringResource(R.string.reports_value_note)) }
                if (report.negative > 0) item { Text(stringResource(R.string.reports_negative, report.negative), style = MaterialTheme.typography.bodySmall) }
                item { SectionLabel(stringResource(R.string.reports_vendor_stock)) }
                item { Text(stringResource(R.string.reports_chart_note), style = MaterialTheme.typography.bodySmall) }
                items(report.byVendor, key = { "vendor:" + it.id }) { vendor ->
                    VendorBar(vendor, report.byVendor.maxOfOrNull { it.units } ?: 0)
                }
                item { SectionLabel(stringResource(R.string.reports_stock_list)) }
                items(report.products, key = { "product:" + it.id }) { product -> ProductCard(product) { onProduct(product.id) } }
            }
        }
    }
    if (showFilter) ProductFilterSheet(filter, state.products, { showFilter = false }) { filter = it; showFilter = false }
}

@Composable
private fun VendorBar(vendor: VendorStock, maximum: Long) {
    val label = stringResource(R.string.reports_vendor_bar, vendor.name, vendor.units)
    Surface(shape = RoundedCornerShape(11.dp), color = Palette.Card,
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = label }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Box(Modifier.fillMaxWidth().height(12.dp).background(Palette.Search, RoundedCornerShape(6.dp))) {
                val fraction = if (maximum > 0) (vendor.units.toDouble() / maximum).toFloat() else 0f
                if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(Palette.Blue, RoundedCornerShape(6.dp)))
            }
        }
    }
}

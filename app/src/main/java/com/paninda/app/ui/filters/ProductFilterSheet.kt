package com.paninda.app.ui.filters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.SectionLabel
import com.paninda.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductFilterSheet(current: ProductFilter, products: List<Product>,
    onDismiss: () -> Unit, onApply: (ProductFilter) -> Unit) {
    var draft by remember { mutableStateOf(current) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).navigationBarsPadding().padding(horizontal = 24.dp)) {
            Text(stringResource(R.string.filters_products), fontFamily = AppFonts.Serif,
                fontStyle = FontStyle.Italic, fontSize = 28.sp)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(top = 20.dp)) {
                SectionLabel(stringResource(R.string.filters_stock))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StockFilter.entries.forEach { stock ->
                        val label = when (stock) {
                            StockFilter.ALL -> R.string.common_all
                            StockFilter.AVAILABLE -> R.string.filters_available
                            StockFilter.LOW -> R.string.filters_low
                            StockFilter.OUT -> R.string.filters_out
                        }
                        FilterChip(draft.stock == stock, { draft = draft.copy(stock = stock) },
                            label = { Text(stringResource(label), fontSize = 16.sp) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.filters_vendor))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(draft.vendorId == null, { draft = draft.copy(vendorId = null) },
                        label = { Text(stringResource(R.string.common_all), fontSize = 16.sp) })
                    products.associate { it.vendorId to it.vendorName }.forEach { (id, name) ->
                        FilterChip(draft.vendorId == id, { draft = draft.copy(vendorId = id) },
                            label = { Text(name, fontSize = 16.sp) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.filters_category))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(draft.category == null, { draft = draft.copy(category = null) },
                        label = { Text(stringResource(R.string.common_all), fontSize = 16.sp) })
                    products.map { it.category }.distinct().sorted().forEach { category ->
                        FilterChip(draft.category == category, { draft = draft.copy(category = category) },
                            label = { Text(category, fontSize = 16.sp) })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton({ draft = ProductFilter() }) { Text(stringResource(R.string.common_clear)) }
                TextButton(onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Button({ onApply(draft) }) { Text(stringResource(R.string.common_apply)) }
            }
        }
    }
}

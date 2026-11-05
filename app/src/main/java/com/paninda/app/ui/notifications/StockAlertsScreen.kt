package com.paninda.app.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockAlertsScreen(products: List<Product>, onBack: () -> Unit, onProduct: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(StockFilter.ALL) }
    val attention = products.filter { it.stockAlertLevel() != StockAlertLevel.HEALTHY }
    val rows = attention.filter { ProductFilter(stock = filter).matches(it, query) }
    PageScaffold(stringResource(R.string.stock_alerts_title), onBack,
        header = {
            SearchField(query, { query = it }, stringResource(R.string.stock_alerts_search))
            FlowRow(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(StockFilter.ALL, StockFilter.LOW, StockFilter.OUT).forEach { value ->
                    FilterChip(filter == value, { filter = value }, label = {
                        Text(stringResource(when (value) {
                            StockFilter.LOW -> R.string.home_low_stock
                            StockFilter.OUT -> R.string.home_out_of_stock
                            else -> R.string.common_all
                        }))
                    })
                }
            }
        }) { padding ->
        if (rows.isEmpty()) EmptyState(stringResource(if (attention.isEmpty()) R.string.stock_alerts_empty else R.string.common_no_matches), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rows, key = { it.id }) { product -> ProductCard(product) { onProduct(product.id) } }
        }
    }
}

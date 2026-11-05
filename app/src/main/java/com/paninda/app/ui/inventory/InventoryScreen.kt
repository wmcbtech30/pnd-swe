package com.paninda.app.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.*
import com.paninda.app.ui.filters.*

@Composable
fun InventoryScreen(products: List<Product>, initialStock: StockFilter = StockFilter.ALL,
    onNew: () -> Unit, onProduct: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberProductFilter(initialStock)
    var showFilter by rememberSaveable { mutableStateOf(false) }
    val rows = products.filter { filter.matches(it, query) }
    val scroll = rememberLazyListState()
    LaunchedEffect(query, filter) { scroll.scrollToItem(0) }
    PageScaffold(
        title = stringResource(R.string.common_inventory),
        actions = { FilterAction(filter.active) { showFilter = true } },
        header = { SearchField(query, { query = it }, stringResource(R.string.inventory_search)) },
        fab = { ActionFab(stringResource(R.string.inventory_new), Icons.Default.Add, onClick = onNew) },
    ) { padding ->
        if (rows.isEmpty()) EmptyState(stringResource(
            if (products.isEmpty()) R.string.inventory_empty else R.string.common_no_matches), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("inventory-list"), state = scroll,
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 6.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(rows, key = { it.id }) { product -> ProductCard(product) { onProduct(product.id) } }
        }
    }
    if (showFilter) ProductFilterSheet(filter, products, { showFilter = false }) {
        filter = it
        showFilter = false
    }
}

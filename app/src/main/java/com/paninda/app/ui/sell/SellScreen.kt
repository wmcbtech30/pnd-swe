package com.paninda.app.ui.sell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.*
import com.paninda.app.ui.filters.*

@Composable
fun SellScreen(products: List<Product>, onRecord: (String?) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberProductFilter()
    var showFilter by rememberSaveable { mutableStateOf(false) }
    val rows = products.filter { filter.matches(it, query) }
    val scroll = rememberLazyListState()
    LaunchedEffect(query, filter) { scroll.scrollToItem(0) }
    PageScaffold(title = stringResource(R.string.common_sell),
        actions = {
            FilterAction(filter.active) { showFilter = true }
        },
        header = { SearchField(query, { query = it }, stringResource(R.string.sell_search)) },
        fab = { ActionFab(stringResource(R.string.sale_record_title), Icons.Default.Add) { onRecord(null) } },
    ) { padding ->
        if (rows.isEmpty()) EmptyState(stringResource(R.string.common_no_matches), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding), state = scroll,
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 0.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)) {
            items(rows, key = { it.id }) { ProductCard(it, selling = true) { onRecord(it.id) } }
        }
    }
    if (showFilter) ProductFilterSheet(filter, products, { showFilter = false }) {
        filter = it
        showFilter = false
    }
}

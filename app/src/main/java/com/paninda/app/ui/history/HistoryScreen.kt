package com.paninda.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.*
import com.paninda.app.ui.filters.*

@Composable
fun HistoryScreen(transactions: List<Transaction>, onReceipt: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberDateFilter()
    var showFilter by rememberSaveable { mutableStateOf(false) }
    val today = rememberShopDate()
    val rows = transactions.filter { filter.matches(it, today) && it.receiptNumber.contains(query.trim(), true) }
    val scroll = rememberLazyListState()
    LaunchedEffect(query, filter) { scroll.scrollToItem(0) }
    PageScaffold(title = stringResource(R.string.common_history),
        actions = { FilterAction(filter.range != DateRange.TODAY) { showFilter = true } },
        header = {
            SearchField(query, { query = it }, stringResource(R.string.history_search))
            Text(stringResource(R.string.history_summary, Money.amount(rows.sumOf { it.totalMinor }),
                pluralStringResource(R.plurals.common_transactions, rows.size, rows.size)),
                modifier = Modifier.padding(start = 32.dp, end = 24.dp, bottom = 18.dp))
        },
    ) { padding ->
        if (rows.isEmpty()) EmptyState(stringResource(
            if (query.isBlank()) R.string.history_empty else R.string.common_no_matches), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding), state = scroll,
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rows, key = { it.id }) { sale -> TransactionCard(sale) { onReceipt(sale.id) } }
        }
    }
    if (showFilter) DateFilterSheet(filter, { showFilter = false }) {
        filter = it
        showFilter = false
    }
}

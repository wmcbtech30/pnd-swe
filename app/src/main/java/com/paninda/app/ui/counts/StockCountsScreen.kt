package com.paninda.app.ui.counts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.*
import com.paninda.app.ui.filters.*
import com.paninda.app.ui.theme.Palette

@Composable
fun StockCountsScreen(counts: List<StockCount>, onBack: () -> Unit, onNew: () -> Unit, onCount: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var dates by rememberDateFilter(DateRange.ALL)
    var showDates by rememberSaveable { mutableStateOf(false) }
    val today = rememberShopDate()
    val rows = counts.filter { dates.matches(it.countedAt, today) &&
        (query.isBlank() || it.countedBy.contains(query, true) || it.id.contains(query, true)) }
    PageScaffold(stringResource(R.string.counts_title), onBack,
        actions = { FilterAction(dates.range != DateRange.ALL) { showDates = true } },
        header = { SearchField(query, { query = it }, stringResource(R.string.counts_search)) },
        fab = { ActionFab(stringResource(R.string.counts_new), Icons.Default.Add, onClick = onNew) }) { padding ->
        if (rows.isEmpty()) EmptyState(stringResource(if (counts.isEmpty()) R.string.counts_empty else R.string.common_no_matches), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rows, key = { it.id }) { count -> Surface(onClick = { onCount(count.id) }, color = Palette.Card,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(dateTimeLabel(count.countedAt), style = MaterialTheme.typography.titleMedium)
                    Text(count.countedBy)
                    if (count.notes.isNotBlank()) Text(count.notes, style = MaterialTheme.typography.bodySmall)
                }
            } }
        }
    }
    if (showDates) DateFilterSheet(dates, { showDates = false }) { dates = it; showDates = false }
}

package com.paninda.app.ui.receiving

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun deliveryDateLabel(time: Long): String = Instant.ofEpochMilli(time).atZone(ShopZone)
    .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))

@Composable
fun ReceivingScreen(deliveries: List<StockDelivery>, onBack: () -> Unit, onNew: () -> Unit, onDelivery: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var dates by rememberDateFilter(DateRange.ALL)
    var showDates by rememberSaveable { mutableStateOf(false) }
    val today = rememberShopDate()
    val rows = deliveries.filter { dates.matches(it.receivedAt, today) && (query.isBlank() ||
        it.vendorNameSnapshot.contains(query.trim(), true) || it.reference.contains(query.trim(), true) || it.receivedBy.contains(query.trim(), true)) }
    val scroll = rememberLazyListState()
    LaunchedEffect(query, dates) { scroll.scrollToItem(0) }
    PageScaffold(stringResource(R.string.receiving_title), onBack,
        actions = { FilterAction(dates.range != DateRange.ALL) { showDates = true } },
        header = { SearchField(query, { query = it }, stringResource(R.string.receiving_search)) },
        fab = { ActionFab(stringResource(R.string.receiving_new), Icons.Default.Add, onClick = onNew) }) { padding ->
        if (rows.isEmpty()) EmptyState(stringResource(if (deliveries.isEmpty()) R.string.receiving_empty else R.string.common_no_matches), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding), state = scroll,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rows, key = { it.id }) { delivery -> Surface(onClick = { onDelivery(delivery.id) }, color = Palette.Card,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(delivery.vendorNameSnapshot, style = MaterialTheme.typography.titleMedium)
                    Text(deliveryDateLabel(delivery.receivedAt))
                    if (delivery.reference.isNotBlank()) Text(delivery.reference, style = MaterialTheme.typography.bodySmall)
                    Text(delivery.receivedBy, style = MaterialTheme.typography.bodySmall)
                }
            } }
        }
    }
    if (showDates) DateFilterSheet(dates, { showDates = false }) { dates = it; showDates = false }
}

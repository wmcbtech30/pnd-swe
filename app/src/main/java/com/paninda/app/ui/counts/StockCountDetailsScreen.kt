package com.paninda.app.ui.counts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.ShopViewModel
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.Palette
import kotlinx.coroutines.CancellationException

@Composable
fun StockCountDetailsScreen(id: String, counts: List<StockCount>, vm: ShopViewModel, onBack: () -> Unit) {
    val count = counts.find { it.id == id }
    var failed by remember(id) { mutableStateOf(false) }
    val items by produceState<List<StockCountItem>?>(null, id) {
        try { value = vm.countItems(id) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    PageScaffold(stringResource(R.string.count_details_title), onBack) { padding ->
        if (count == null) EmptyState(stringResource(R.string.count_details_missing), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                DetailField(stringResource(R.string.count_details_date), dateTimeLabel(count.countedAt))
                DetailField(stringResource(R.string.count_editor_by), count.countedBy)
                if (count.notes.isNotBlank()) DetailField(stringResource(R.string.count_details_notes), count.notes)
                Text(stringResource(R.string.count_details_immutable), style = MaterialTheme.typography.bodySmall)
            }
            if (failed || items == null) item { Text(stringResource(if (failed) R.string.common_load_failed else R.string.common_loading)) }
            items(items.orEmpty(), key = { it.id }) { item -> Surface(color = if (item.variance == 0L) Palette.Card else Palette.ProductWarning,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.productNameSnapshot, style = MaterialTheme.typography.titleMedium)
                    Text(item.skuSnapshot, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.count_details_quantities, item.expectedQuantity, item.countedQuantity))
                    Text(stringResource(R.string.count_editor_variance, signedCount(item.variance)))
                }
            } }
        }
    }
}

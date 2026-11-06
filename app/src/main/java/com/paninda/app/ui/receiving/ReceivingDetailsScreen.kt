package com.paninda.app.ui.receiving

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.ShopViewModel
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.Palette
import kotlinx.coroutines.CancellationException

@Composable
fun ReceivingDetailsScreen(id: String, deliveries: List<StockDelivery>, vm: ShopViewModel, onBack: () -> Unit, onProduct: (String) -> Unit) {
    val delivery = deliveries.find { it.id == id }
    var failed by remember(id) { mutableStateOf(false) }
    val lines by produceState<List<StockDeliveryItem>?>(null, id) {
        try { value = vm.deliveryItems(id) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    PageScaffold(stringResource(R.string.receiving_details_title), onBack) { padding ->
        if (delivery == null) EmptyState(stringResource(R.string.receiving_details_missing), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(delivery.vendorNameSnapshot, style = MaterialTheme.typography.titleLarge)
                DetailField(stringResource(R.string.receiving_details_date), deliveryDateLabel(delivery.receivedAt))
                DetailField(stringResource(R.string.receiving_details_recorded), dateTimeLabel(delivery.recordedAt))
                DetailField(stringResource(R.string.receiving_editor_by), delivery.receivedBy)
                if (delivery.reference.isNotBlank()) DetailField(stringResource(R.string.receiving_details_reference), delivery.reference)
                if (delivery.notes.isNotBlank()) DetailField(stringResource(R.string.receiving_details_notes), delivery.notes)
                Text(stringResource(R.string.receiving_details_immutable), style = MaterialTheme.typography.bodySmall)
            }
            if (failed || lines == null) item { Text(stringResource(if (failed) R.string.common_load_failed else R.string.common_loading)) }
            items(lines.orEmpty(), key = { it.id }) { line -> Surface(onClick = { onProduct(line.productId) }, color = Palette.Card,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(line.productNameSnapshot, style = MaterialTheme.typography.titleMedium)
                    Text(line.skuSnapshot, style = MaterialTheme.typography.bodySmall)
                    Text(pluralStringResource(R.plurals.receiving_details_units, line.quantity, line.quantity))
                }
            } }
        }
    }
}

package com.paninda.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.ShopViewModel
import com.paninda.app.ui.components.*
import kotlinx.coroutines.CancellationException

@Composable
fun TransactionDetailsScreen(transactionId: String, transactions: List<Transaction>, vm: ShopViewModel, onBack: () -> Unit) {
    val transaction = transactions.find { it.id == transactionId }
    var failed by remember(transactionId) { mutableStateOf(false) }
    val lines by produceState(emptyList<TransactionItem>(), transactionId) {
        try { value = vm.items(transactionId) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    PageScaffold(stringResource(R.string.transaction_details_title), onBack = onBack) { padding ->
        if (transaction == null) EmptyState(stringResource(R.string.transaction_details_missing), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 12.dp)) {
            item {
                Text(transaction.receiptNumber, fontSize = 32.sp, lineHeight = 40.sp)
                if (transaction.syncState == SyncState.PENDING) PendingBadge()
                DetailField(stringResource(R.string.transaction_details_date), dateTimeLabel(transaction.soldAt))
                DetailField(stringResource(R.string.transaction_details_cashier), transaction.cashierName)
                DetailField(stringResource(R.string.transaction_details_payment), paymentLabel(transaction.paymentMethod))
                SectionLabel(stringResource(R.string.transaction_details_items))
            }
            if (failed) item { Text(stringResource(R.string.common_load_failed)) }
            items(lines, key = { it.id }) { line ->
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(line.productNameSnapshot, fontSize = 20.sp)
                    Row {
                        Text(stringResource(R.string.transaction_details_line, line.quantity, Money.amount(line.unitPriceMinor)),
                            fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.common_amount, Money.amount(line.lineTotalMinor)), fontSize = 18.sp)
                    }
                }
            }
            item {
                DetailField(stringResource(R.string.transaction_details_total),
                    stringResource(R.string.common_amount, Money.amount(transaction.totalMinor)))
                if (transaction.paymentMethod == PaymentMethod.CASH && transaction.amountTenderedMinor != null) {
                    DetailField(stringResource(R.string.transaction_details_tendered),
                        stringResource(R.string.common_amount, Money.amount(transaction.amountTenderedMinor)))
                    DetailField(stringResource(R.string.transaction_details_change),
                        stringResource(R.string.common_amount, Money.amount(transaction.amountTenderedMinor - transaction.totalMinor)))
                }
            }
        }
    }
}

package com.paninda.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.*
import com.paninda.app.ui.sync.lastSyncText

@Composable
fun HomeScreen(state: ShopState, onSell: () -> Unit, onHistory: () -> Unit,
    onReceipt: (String) -> Unit, onStock: (StockFilter) -> Unit, onSync: () -> Unit) {
    val today = rememberShopDate()
    val transactions = state.transactions.filter { DateFilter().matches(it, today) }
    val low = state.products.count { it.isActive && it.isLowStock }
    val out = state.products.count { it.isActive && it.isOutOfStock }
    PageScaffold(title = rememberHomeGreeting(), subtitle = lastSyncText(state.syncSummary),
        actions = {
            IconButton(onSync) { Icon(Icons.Default.Refresh, stringResource(R.string.home_refresh),
                Modifier.size(32.dp), tint = Palette.Icon) }
        },
        fab = { ActionFab(stringResource(R.string.home_new_sale), Icons.Default.Add, onClick = onSell) },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val compact = maxHeight < 440.dp
            val numericStock = compact || maxWidth < 380.dp || LocalDensity.current.fontScale > 1.2f
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                SalesCard(transactions.sumOf { it.totalMinor }, transactions.size, compact, onHistory)
                Spacer(Modifier.height(if (compact) 8.dp else 13.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    val healthy = low == 0 && out == 0 && state.products.any { it.isActive }
                    StockCard(
                        stringResource(if (healthy) R.string.home_healthy_title else R.string.home_low_stock),
                        if (healthy) stringResource(R.string.home_healthy)
                        else if (numericStock) low.toString() else pluralStringResource(R.plurals.common_products, low, low),
                        low > 0, compact, Modifier.weight(195f),
                    ) { onStock(StockFilter.LOW) }
                    StockCard(stringResource(R.string.home_out_of_stock), out.toString(), out > 0,
                        compact, Modifier.weight(164f), FontWeight.Normal) { onStock(StockFilter.OUT) }
                }
                Spacer(Modifier.height(if (compact) 10.dp else 28.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_recent), fontFamily = AppFonts.Condensed,
                        fontWeight = FontWeight.Light, fontSize = 24.sp, lineHeight = 30.sp,
                        modifier = Modifier.weight(1f))
                    TextButton(onHistory, contentPadding = PaddingValues(0.dp)) {
                        Text(stringResource(R.string.home_view_all), fontFamily = AppFonts.Condensed,
                            fontSize = 24.sp, color = Palette.Link)
                    }
                }
                if (state.transactions.isEmpty()) {
                    if (!compact) EmptyState(stringResource(R.string.home_recent_empty),
                        Modifier.weight(1f).padding(bottom = 88.dp))
                } else LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 18.dp, top = 6.dp, end = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.transactions.take(20), key = { it.id }) { sale ->
                        Text(stringResource(R.string.home_recent_line, sale.receiptNumber,
                            timeLabel(sale.soldAt), Money.amount(sale.totalMinor)),
                            fontFamily = AppFonts.Condensed, fontSize = 16.sp, lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth().clickable { onReceipt(sale.id) }.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesCard(total: Long, count: Int, compact: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Palette.Card)
        .clickable(onClick = onClick).heightIn(min = if (compact) 100.dp else 171.dp).padding(if (compact) 6.dp else 14.dp)) {
        Row {
            Text(stringResource(R.string.home_today_sales), fontFamily = AppFonts.Serif,
                fontStyle = FontStyle.Italic, fontSize = if (compact) 14.sp else 16.sp,
                lineHeight = if (compact) 18.sp else 24.sp, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.home_total), fontFamily = AppFonts.Condensed,
                fontSize = if (compact) 14.sp else 16.sp, lineHeight = if (compact) 18.sp else 24.sp)
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
            Text(stringResource(R.string.home_currency), fontFamily = AppFonts.Condensed,
                fontSize = 16.sp, modifier = Modifier.padding(top = 14.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(Money.amount(total), fontFamily = AppFonts.Condensed, fontWeight = FontWeight.Thin,
                    fontSize = if (compact) 40.sp else 64.sp, lineHeight = if (compact) 48.sp else 76.sp)
                Text(pluralStringResource(R.plurals.common_transactions, count, count),
                    fontFamily = AppFonts.Condensed, fontWeight = FontWeight.Thin,
                    fontSize = if (compact) 16.sp else 24.sp, lineHeight = if (compact) 20.sp else 30.sp)
            }
        }
    }
}

@Composable
private fun StockCard(title: String, value: String, warning: Boolean, compact: Boolean,
    modifier: Modifier, valueWeight: FontWeight = FontWeight.Thin, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(11.dp))
        .background(if (warning) Palette.StockWarning else Palette.Card)
        .clickable(onClick = onClick).heightIn(min = if (compact) 60.dp else 87.dp)
        .padding(start = 14.dp, end = 8.dp, top = if (compact) 7.dp else 10.dp, bottom = if (compact) 7.dp else 10.dp)) {
        Text(title, fontFamily = AppFonts.Serif, fontStyle = FontStyle.Italic,
            fontSize = if (compact) 13.sp else 15.sp, lineHeight = if (compact) 18.sp else 20.sp)
        Text(value, fontFamily = AppFonts.Condensed, fontWeight = valueWeight,
            fontSize = if (compact) 24.sp else 40.sp, lineHeight = if (compact) 28.sp else 48.sp)
    }
}

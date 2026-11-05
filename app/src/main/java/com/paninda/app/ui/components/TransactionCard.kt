package com.paninda.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.theme.*

@Composable
fun TransactionCard(transaction: Transaction, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Palette.Card)
        .clickable(onClick = onClick).heightIn(min = 126.dp).padding(13.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(transaction.receiptNumber, fontFamily = AppFonts.Condensed, fontWeight = FontWeight.Light,
                fontSize = 32.sp, lineHeight = 38.sp, modifier = Modifier.weight(1f))
            if (transaction.syncState == SyncState.PENDING) PendingBadge()
        }
        Text(stringResource(R.string.history_metadata, timeLabel(transaction.soldAt), paymentLabel(transaction.paymentMethod)),
            fontFamily = AppFonts.Condensed, fontWeight = FontWeight.Light, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(12.dp))
        Row {
            Text(stringResource(R.string.common_amount, Money.amount(transaction.totalMinor)),
                fontFamily = AppFonts.Condensed, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Text(pluralStringResource(R.plurals.common_items, transaction.itemCount, transaction.itemCount),
                fontFamily = AppFonts.Condensed, fontSize = 20.sp)
        }
    }
}

@Composable
fun PendingBadge() {
    Text(stringResource(R.string.common_pending), color = Palette.White, fontFamily = AppFonts.Condensed,
        fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.clip(RoundedCornerShape(24.dp))
            .background(Palette.Blue).padding(horizontal = 10.dp, vertical = 4.dp))
}

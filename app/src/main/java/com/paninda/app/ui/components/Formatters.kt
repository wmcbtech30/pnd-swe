package com.paninda.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paninda.app.R
import com.paninda.app.domain.model.PaymentMethod
import com.paninda.app.domain.model.ShopZone
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

fun timeLabel(time: Long): String = Instant.ofEpochMilli(time).atZone(ShopZone)
    .format(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH))
fun dateTimeLabel(time: Long): String = Instant.ofEpochMilli(time).atZone(ShopZone)
    .format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.ENGLISH))

@Composable
fun paymentLabel(method: PaymentMethod): String = stringResource(
    when (method) {
        PaymentMethod.CASH -> R.string.history_cash
        PaymentMethod.MANUAL_OTHER -> R.string.history_manual
        PaymentMethod.NOT_RECORDED -> R.string.sale_record_payment_none
    })

package com.paninda.app.domain.model

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Money {
    fun parse(value: String): Long? {
        val clean = value.trim()
        if (!Regex("""\d{1,7}(\.\d{1,2})?""").matches(clean)) return null
        return runCatching { BigDecimal(clean).movePointRight(2).longValueExact() }.getOrNull()
    }

    fun amount(minor: Long): String =
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(BigDecimal.valueOf(minor, 2))

    fun editable(minor: Long): String = BigDecimal.valueOf(minor, 2).toPlainString()
}

package com.paninda.app.ui.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.paninda.app.domain.model.*

private val ProductSaver = listSaver<ProductFilter, String>(
    save = { listOf(it.stock.name, it.vendorId.orEmpty(), it.category.orEmpty()) },
    restore = { ProductFilter(StockFilter.valueOf(it[0]), it[1].ifEmpty { null }, it[2].ifEmpty { null }) })
private val DateSaver = listSaver<DateFilter, String>(
    save = { listOf(it.range.name, it.start, it.end) },
    restore = { DateFilter(DateRange.valueOf(it[0]), it[1], it[2]) })

@Composable
fun rememberProductFilter(initial: StockFilter = StockFilter.ALL): MutableState<ProductFilter> =
    rememberSaveable(stateSaver = ProductSaver) { mutableStateOf(ProductFilter(initial)) }

@Composable
fun rememberDateFilter(initial: DateRange = DateRange.TODAY): MutableState<DateFilter> =
    rememberSaveable(stateSaver = DateSaver) { mutableStateOf(DateFilter(initial)) }

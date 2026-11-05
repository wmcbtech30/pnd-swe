package com.paninda.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.theme.*

@Composable
fun ProductCard(product: Product, selling: Boolean = false, onClick: () -> Unit) {
    val warning = product.isOutOfStock || (!selling && product.isLowStock)
    Column(Modifier.fillMaxWidth().testTag("product-" + product.id).clip(RoundedCornerShape(10.dp))
        .background(if (warning) Palette.ProductWarning else Palette.Card)
        .then(if (!selling) Modifier.clickable(onClick = onClick) else Modifier)
        .heightIn(min = 126.dp).padding(horizontal = 13.dp, vertical = if (selling) 24.dp else 16.dp)) {
        Text(product.name, fontFamily = AppFonts.Condensed, fontWeight = FontWeight.Light,
            fontSize = 32.sp, lineHeight = 38.sp)
        if (!selling) Text(product.vendorName, fontFamily = AppFonts.Condensed,
            fontWeight = FontWeight.Light, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(if (selling) 10.dp else 12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(stringResource(R.string.common_amount, Money.amount(product.unitPriceMinor)),
                fontFamily = AppFonts.Condensed, fontSize = 20.sp, modifier = Modifier.weight(1f))
            val stock = when {
                product.isOutOfStock -> stringResource(R.string.inventory_out)
                selling -> pluralStringResource(R.plurals.sell_available, product.quantity, product.quantity)
                product.isLowStock -> stringResource(R.string.inventory_low, product.quantity)
                else -> stringResource(R.string.inventory_stock, product.quantity)
            }
            Text(stock, fontFamily = AppFonts.Condensed, fontSize = 20.sp,
                fontWeight = if (selling && !product.isOutOfStock) FontWeight.Thin else FontWeight.Normal)
            if (selling && !product.isOutOfStock) {
                Spacer(Modifier.width(12.dp))
                IconButton(onClick, Modifier.size(42.dp).background(Palette.Blue, CircleShape)) {
                    Icon(Icons.Default.Add, stringResource(R.string.sell_add, product.name),
                        Modifier.size(24.dp), tint = Palette.White)
                }
            }
        }
    }
}

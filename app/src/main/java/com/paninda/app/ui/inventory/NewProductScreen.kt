package com.paninda.app.ui.inventory

import androidx.compose.runtime.Composable
import com.paninda.app.domain.model.Product
import com.paninda.app.ui.ShopViewModel

@Composable
fun NewProductScreen(products: List<Product>, vm: ShopViewModel, onBack: () -> Unit) {
    ProductEditorScreen(null, products, vm, onBack)
}

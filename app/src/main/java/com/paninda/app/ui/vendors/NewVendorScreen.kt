package com.paninda.app.ui.vendors

import androidx.compose.runtime.Composable
import com.paninda.app.domain.model.Vendor
import com.paninda.app.ui.ShopViewModel

@Composable
fun NewVendorScreen(vendors: List<Vendor>, vm: ShopViewModel, onBack: () -> Unit) = VendorEditorScreen(null, vendors, vm, onBack)

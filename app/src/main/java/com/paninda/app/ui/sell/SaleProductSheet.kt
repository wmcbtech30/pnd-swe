package com.paninda.app.ui.sell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.Product
import com.paninda.app.ui.components.SearchField
import com.paninda.app.ui.theme.Palette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SaleProductSheet(products: List<Product>, onDismiss: () -> Unit, onSelect: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.White) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.8f).navigationBarsPadding()) {
            Text(stringResource(R.string.sale_record_add), Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
            SearchField(query, { query = it }, stringResource(R.string.sell_search))
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp)) {
                val rows = products.filter { it.name.contains(query, true) || it.sku.contains(query, true) }
                if (rows.isEmpty()) item { Text(stringResource(R.string.common_no_matches)) }
                items(rows, key = { it.id }) { product -> TextButton({ onSelect(product) }, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sale_record_product, product.name, product.sku, product.quantity))
                } }
            }
        }
    }
}

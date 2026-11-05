package com.paninda.app.ui.vendors

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.ShopViewModel
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Composable
fun VendorEditorScreen(id: String?, vendors: List<Vendor>, vm: ShopViewModel, onBack: () -> Unit) {
    val vendor = vendors.find { it.id == id }
    val original = remember(id) { VendorDraft(vendor?.name.orEmpty(), vendor?.contactPerson.orEmpty(), vendor?.phone.orEmpty(),
        vendor?.email.orEmpty(), vendor?.address.orEmpty(), vendor?.notes.orEmpty()) }
    var name by rememberSaveable(id) { mutableStateOf(original.name) }
    var contact by rememberSaveable(id) { mutableStateOf(original.contactPerson) }
    var phone by rememberSaveable(id) { mutableStateOf(original.phone) }
    var email by rememberSaveable(id) { mutableStateOf(original.email) }
    var address by rememberSaveable(id) { mutableStateOf(original.address) }
    var notes by rememberSaveable(id) { mutableStateOf(original.notes) }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableIntStateOf(0) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val draft = VendorDraft(name, contact, phone, email, address, notes)
    val scope = rememberCoroutineScope()
    val back = { if (draft != original) discard = true else onBack() }
    BackHandler { if (!busy) back() }
    PageScaffold(stringResource(if (id == null) R.string.vendor_editor_new else R.string.vendor_editor_edit),
        onBack = { if (!busy) back() }, fab = {
            if (id == null || vendor != null) ActionFab(stringResource(if (busy) R.string.common_saving else R.string.vendor_editor_save),
                Icons.Default.Check, !busy) {
                busy = true; error = 0
                scope.launch(Dispatchers.Main.immediate) {
                    when (vm.saveVendor(id, draft)) {
                        is SaveVendorResult.Saved -> onBack()
                        SaveVendorResult.Invalid -> error = R.string.vendor_editor_invalid
                        SaveVendorResult.DuplicateName -> error = R.string.vendor_editor_duplicate
                        SaveVendorResult.Missing -> error = R.string.vendor_details_missing
                        SaveVendorResult.Failed -> error = R.string.common_action_failed
                    }
                    busy = false
                }
            }
        }) { padding ->
        if (id != null && vendor == null) EmptyState(stringResource(R.string.vendor_details_missing), Modifier.padding(padding))
        else LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().testTag("vendor-editor-list"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (error != 0) item { Surface(color = Palette.ProductWarning) { Text(stringResource(error), Modifier.padding(12.dp)) } }
            item { VendorField(name, { name = it.take(120) }, R.string.vendor_editor_name, !busy) }
            item { Text(stringResource(R.string.vendor_editor_optional), style = MaterialTheme.typography.bodySmall) }
            item { VendorField(contact, { contact = it.take(120) }, R.string.vendor_editor_contact, !busy) }
            item { VendorField(phone, { phone = it.take(40) }, R.string.vendor_editor_phone, !busy, KeyboardType.Phone) }
            item { VendorField(email, { email = it.take(254) }, R.string.vendor_editor_email, !busy, KeyboardType.Email) }
            item { VendorField(address, { address = it.take(300) }, R.string.vendor_editor_address, !busy, multiline = true) }
            item { VendorField(notes, { notes = it.take(1000) }, R.string.vendor_editor_notes, !busy, multiline = true) }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, containerColor = Palette.White,
        title = { Text(stringResource(R.string.product_editor_unsaved_title)) },
        text = { Text(stringResource(R.string.product_editor_unsaved_body)) },
        confirmButton = { TextButton({ discard = false; onBack() }) { Text(stringResource(R.string.product_editor_discard)) } },
        dismissButton = { TextButton({ discard = false }) { Text(stringResource(R.string.product_editor_keep)) } })
}

@Composable
private fun VendorField(value: String, onChange: (String) -> Unit, label: Int, enabled: Boolean,
    keyboard: KeyboardType = KeyboardType.Text, multiline: Boolean = false) {
    OutlinedTextField(value, onChange, enabled = enabled, label = { Text(stringResource(label), style = MaterialTheme.typography.bodySmall) },
        singleLine = !multiline, minLines = if (multiline) 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = Modifier.fillMaxWidth())
}

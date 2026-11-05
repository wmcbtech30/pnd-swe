package com.paninda.app.ui.connection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.adapters.sheets.*
import com.paninda.app.ui.components.PageScaffold

@Composable
fun WorkbookSelectionScreen(account: GoogleAccountInfo, workbooks: List<DriveWorkbook>, busy: Boolean,
    onBack: () -> Unit, onCreate: () -> Unit, onSelect: (DriveWorkbook) -> Unit) {
    BackHandler(onBack = onBack)
    PageScaffold(stringResource(R.string.workbooks_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Text(account.email, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.workbooks_scope), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onCreate, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.workbooks_create))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.workbooks_saved), style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (workbooks.isEmpty()) item {
                    Text(stringResource(R.string.workbooks_empty), Modifier.padding(vertical = 18.dp), style = MaterialTheme.typography.bodyMedium)
                }
                items(workbooks, key = { it.id }) { workbook ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { onSelect(workbook) }
                        .padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.TableChart, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(workbook.name)
                            Text(workbook.id.takeLast(8), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

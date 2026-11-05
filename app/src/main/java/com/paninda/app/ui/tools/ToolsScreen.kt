package com.paninda.app.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.ui.components.*

@Composable
fun ToolsScreen(onReports: () -> Unit, onVendors: () -> Unit, onSync: () -> Unit, onSettings: () -> Unit, onPending: (Int) -> Unit) {
    PageScaffold(stringResource(R.string.common_tools)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(start = 26.dp, end = 22.dp, top = 12.dp, bottom = 24.dp)) {
            SectionLabel(stringResource(R.string.tools_management))
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(R.string.tools_vendors), Icons.Outlined.Storefront) { onVendors() }
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(R.string.tools_stock_counts), Icons.Outlined.FactCheck) { onPending(R.string.tools_counts_pending) }
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.tools_insights))
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(R.string.tools_reports), Icons.Outlined.BarChart) { onReports() }
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.tools_application))
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(R.string.tools_sync), Icons.Outlined.Sync, onClick = onSync)
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(R.string.tools_settings), Icons.Outlined.Settings, onClick = onSettings)
        }
    }
}

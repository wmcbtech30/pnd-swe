package com.paninda.app.ui.filters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.domain.model.*
import com.paninda.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DateFilterSheet(current: DateFilter, onDismiss: () -> Unit, onApply: (DateFilter) -> Unit) {
    var draft by remember { mutableStateOf(current) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).navigationBarsPadding().padding(horizontal = 24.dp)) {
            Text(stringResource(R.string.filters_dates), fontFamily = AppFonts.Serif,
                fontStyle = FontStyle.Italic, fontSize = 28.sp)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateRange.entries.forEach { range ->
                        val label = when (range) {
                            DateRange.TODAY -> R.string.filters_today
                            DateRange.WEEK -> R.string.filters_week
                            DateRange.MONTH -> R.string.filters_month
                            DateRange.ALL -> R.string.filters_all_dates
                            DateRange.CUSTOM -> R.string.filters_custom
                        }
                        FilterChip(draft.range == range, { draft = draft.copy(range = range) },
                            label = { Text(stringResource(label), fontSize = 16.sp) })
                    }
                }
                if (draft.range == DateRange.CUSTOM) {
                    OutlinedTextField(draft.start, { draft = draft.copy(start = it.take(10)) },
                        label = { Text(stringResource(R.string.filters_start), fontSize = 14.sp) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(draft.end, { draft = draft.copy(end = it.take(10)) },
                        label = { Text(stringResource(R.string.filters_end), fontSize = 14.sp) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (!draft.valid) Text(stringResource(R.string.filters_invalid),
                        fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton({ draft = DateFilter(range = DateRange.ALL) }) { Text(stringResource(R.string.common_clear)) }
                TextButton(onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Button({ onApply(draft) }, enabled = draft.valid) { Text(stringResource(R.string.common_apply)) }
            }
        }
    }
}

package com.paninda.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.ui.theme.*

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val focus = LocalFocusManager.current
    BasicTextField(
        value, onValueChange, singleLine = true,
        textStyle = TextStyle(fontFamily = AppFonts.Sans, fontSize = 20.sp, lineHeight = 24.sp,
            color = Palette.Ink),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        modifier = Modifier.padding(horizontal = 22.dp).fillMaxWidth().heightIn(min = 44.dp)
            .clip(CircleShape).background(Palette.Search),
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = Palette.Hint, fontSize = 20.sp)
                inner()
            }
        },
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
fun FilterAction(active: Boolean, onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(Icons.Outlined.FilterAlt, stringResource(R.string.common_filters),
            Modifier.size(32.dp), tint = if (active) Palette.Blue else Palette.Icon)
    }
}

@Composable
fun ActionFab(label: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, shape = CircleShape,
        modifier = Modifier.heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Palette.Blue,
            contentColor = Palette.White, disabledContainerColor = Palette.Disabled,
            disabledContentColor = Palette.White)) {
        Icon(icon, null, Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 24.sp, lineHeight = 30.sp)
    }
}

@Composable
fun MenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Palette.Card)
        .clickable(onClick = onClick).heightIn(min = 44.dp)
        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Palette.Icon)
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 20.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SectionLabel(label: String) {
    Text(label, color = Palette.Section, fontFamily = AppFonts.Condensed,
        fontSize = 20.sp, lineHeight = 24.sp)
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Palette.Section, fontSize = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun DetailField(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Palette.Section, fontSize = 14.sp, lineHeight = 20.sp)
        Text(value, fontSize = 20.sp)
    }
}

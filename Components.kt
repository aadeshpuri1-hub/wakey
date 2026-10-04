package app.upwake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.upwake.ui.theme.W
import app.upwake.ui.theme.WIcons

/** White pill button with black text. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) W.Accent else W.Card2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val c = if (enabled) W.OnAccent else W.Text3
        if (icon != null) {
            Icon(icon, null, tint = c, modifier = Modifier.size(20.dp))
            Box(Modifier.width(10.dp))
        }
        Text(text, color = c, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** Grey-black button with white text. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(W.Card2)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = W.Text, modifier = Modifier.size(18.dp))
            Box(Modifier.width(10.dp))
        }
        Text(text, color = W.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Small pill, e.g. "Fix". */
@Composable
fun Pill(text: String, onClick: () -> Unit, filled: Boolean = true) {
    Text(
        text,
        color = if (filled) W.OnAccent else W.Text,
        fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) W.Accent else W.Card2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
fun WSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = W.Green,
            checkedBorderColor = W.Green,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = W.Card2,
            uncheckedBorderColor = W.Card2,
        ),
    )
}

/** Grey-black rounded group, like iOS / Alarmy settings groups. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(W.Card),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = W.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        modifier = modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
fun RowDivider() {
    HorizontalDivider(color = W.Line, thickness = 1.dp, modifier = Modifier.padding(start = 54.dp))
}

/** One tappable settings row: icon · title · value · trailing (chevron by default). */
@Composable
fun SettingRow(
    icon: ImageVector?,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = W.Text, modifier = Modifier.size(22.dp))
            Box(Modifier.width(16.dp))
        }
        Text(title, color = W.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(
                value, color = W.Text2, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp).weight(1f, fill = false),
            )
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(WIcons.Chevron, null, tint = W.Text3, modifier = Modifier.padding(start = 6.dp).size(18.dp))
        }
    }
}

/** Black bottom sheet with a title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = W.Card,
        contentColor = W.Text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = W.Text3) },
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            Text(
                title, color = W.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            content()
        }
    }
}

/** Row of selectable chips (monochrome segmented control). */
@Composable
fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) W.Accent else W.Card2)
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o), color = if (on) W.OnAccent else W.Text2,
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                )
            }
        }
    }
}

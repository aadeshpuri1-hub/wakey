package app.upwake.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.upwake.gratitude.GratitudeStore
import app.upwake.ui.theme.W
import app.upwake.ui.theme.WIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    BackHandler(onBack = onBack)
    GratitudeStore.init(ctx)
    val entries by GratitudeStore.entries.collectAsState()
    val fmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    var deleting by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize().background(W.Bg).systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(WIcons.Close, "Close", tint = W.Text, modifier = Modifier.size(22.dp)) }
            Text("Gratitude journal", color = W.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "${entries.size} morning${if (entries.size == 1) "" else "s"} of gratitude",
                    color = W.Text2, fontSize = 15.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                )
            }
            if (entries.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(80.dp).clip(CircleShape).background(W.Card),
                            contentAlignment = Alignment.Center,
                        ) { Icon(WIcons.Book, null, tint = W.Text, modifier = Modifier.size(36.dp)) }
                        Spacer(Modifier.height(16.dp))
                        Text("Nothing yet", color = W.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Your entries appear here after each morning alarm.",
                            color = W.Text2, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            items(entries, key = { it.time }) { e ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(W.Card).padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            fmt.format(Date(e.time)), color = W.Text2, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).clickable { deleting = e.time },
                            contentAlignment = Alignment.Center,
                        ) { Icon(WIcons.Trash, "Delete", tint = W.Text2, modifier = Modifier.size(18.dp)) }
                    }
                    if (e.topic.isNotBlank()) {
                        Text(
                            e.topic, color = W.Text2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Text(
                        e.text, color = W.Text, fontSize = 16.sp, lineHeight = 23.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = W.Card,
            title = { Text("Delete this entry?", color = W.Text) },
            text = { Text("It will be removed from your journal for good.", color = W.Text2) },
            confirmButton = {
                TextButton(onClick = {
                    GratitudeStore.delete(ctx, t)
                    deleting = null
                }) { Text("Delete", color = W.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel", color = W.Dawn) }
            },
        )
    }
}

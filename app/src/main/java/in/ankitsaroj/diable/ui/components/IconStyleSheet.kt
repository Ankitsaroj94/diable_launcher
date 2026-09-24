package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import `in`.ankitsaroj.diable.data.IconPackRepository
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.InstalledIconPack
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.model.AppInfo
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Accents offered right in the icon sheet, so the colour-following styles can be tried. */
private val IconAccents = listOf(
    0xFFB4C5FF, 0xFF9BB6F8, 0xFF6DD5ED, 0xFFA8D5BA, 0xFFF2C6C2,
    0xFFFFB86C, 0xFFC9A7EB, 0xFF00DBDE, 0xFFD8D8D8,
).map { it.toInt() }

/**
 * App icons: a built-in style (shape + colour treatment that follows the theme accent)
 * and, optionally, an installed third-party icon pack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconStyleSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    LaunchedEffect(Unit) { repos.apps.ensureLoaded() }
    val scope = rememberCoroutineScope()
    val current = IconStyle.fromKey(prefs.iconStyleKey)
    val accent = Color(prefs.accentColorArgb)

    var packs by remember { mutableStateOf<List<InstalledIconPack>?>(null) }
    LaunchedEffect(Unit) {
        packs = withContext(Dispatchers.IO) { IconPackRepository.installed(context) }
    }

    // Preview with the user's own favorites so each style is judged on real icons.
    val samples = remember(apps, prefs.favoritePackages) {
        val favs = apps.filter { it.packageName in prefs.favoritePackages }
        (favs + apps).distinctBy { it.packageName }.take(3)
    }

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = "App icons",
                modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 12.dp),
                fontSize = 23.sp,
                color = DiableText,
            )

            SectionTitle("Style")
            IconStyle.entries.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { style ->
                        IconStyleOption(
                            style = style,
                            samples = samples,
                            accent = accent,
                            selected = style == current,
                            onClick = {
                                repos.prefs.launchUpdate { it.copy(iconStyleKey = style.key) }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }

            SectionTitle(
                if (current.followsAccent) "Icon colour" else "Accent colour",
                subtitle = if (current.followsAccent) {
                    null
                } else {
                    "Pick Glyph, Themed, Midnight or Duotone to colour icons with it"
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconAccents.forEach { argb ->
                    val selected = argb == prefs.accentColorArgb
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .then(if (selected) Modifier.border(2.dp, DiableText, CircleShape) else Modifier)
                            .clickable {
                                repos.prefs.launchUpdate { it.copy(accentColorArgb = argb) }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(Icons.Default.Check, null, tint = Color(0xFF132569), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            SectionTitle("Icon pack", subtitle = "Installed icon packs replace app artwork")
            IconPackRow(
                label = "Built-in style",
                icon = null,
                selected = prefs.iconPackPackage == null,
                onClick = { repos.prefs.launchUpdate { it.copy(iconPackPackage = null) } },
            )
            when (val list = packs) {
                null -> Unit
                else -> list.forEach { pack ->
                    IconPackRow(
                        label = pack.label,
                        icon = pack,
                        selected = prefs.iconPackPackage == pack.packageName,
                        onClick = {
                            repos.prefs.launchUpdate { it.copy(iconPackPackage = pack.packageName) }
                        },
                    )
                }
            }
            if (packs?.isEmpty() == true) {
                Text(
                    text = "No icon packs installed.",
                    color = DiableTextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                )
            }
            DiableSettingCard(
                icon = Icons.Default.Download,
                title = "Get icon packs",
                subtitle = "Any ADW / Nova compatible pack works",
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=icon%20pack&c=apps"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }.onFailure {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/search?q=icon%20pack&c=apps"),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)) {
        Text(text = text, color = DiableText, fontSize = 17.sp)
        if (subtitle != null) {
            Text(text = subtitle, color = DiableTextMuted, fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun IconStyleOption(
    style: IconStyle,
    samples: List<AppInfo>,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) DiableAccent.copy(alpha = 0.18f) else DiableCard)
            .then(
                if (selected) Modifier.border(2.dp, DiableAccent, RoundedCornerShape(16.dp)) else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            samples.forEach { app ->
                AppIcon(app = app, size = 26.dp, iconStyle = style, accentColor = accent)
            }
        }
        Text(
            text = style.label,
            color = if (selected) DiableText else DiableTextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun IconPackRow(
    label: String,
    icon: InstalledIconPack?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DiableCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon?.icon != null) {
            Image(
                painter = rememberDrawablePainter(icon.icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        } else {
            LavenderIconCircle(Icons.Default.Palette, size = 32.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = DiableText, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) DiableAccent else DiableTextMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(DiableAccent))
            }
        }
    }
}

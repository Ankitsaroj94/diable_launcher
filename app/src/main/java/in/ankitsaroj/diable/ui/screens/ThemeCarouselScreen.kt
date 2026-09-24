package `in`.ankitsaroj.diable.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import `in`.ankitsaroj.diable.ui.components.AppIcon
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.ThemePreset
import `in`.ankitsaroj.diable.data.ThemePresets
import `in`.ankitsaroj.diable.data.DiableRepos
import `in`.ankitsaroj.diable.data.WebWallpaper
import `in`.ankitsaroj.diable.ui.home.ClockStylePreview
import `in`.ankitsaroj.diable.ui.theme.EditorChrome
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import `in`.ankitsaroj.diable.ui.theme.ThemeFont
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Ready-made themes. Each card previews a full look — wallpaper, typeface, clock face,
 * accent and icon shape — and "Apply theme" commits all of them at once.
 */
@Composable
fun ThemeCarouselScreen(navController: NavController) {
    val repos = LocalDiableRepos.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val presets = ThemePresets.all
    val imageLoader = context.imageLoader

    // One representative wallpaper per preset, fetched lazily for the preview.
    val covers = remember { mutableStateMapOf<String, WebWallpaper?>() }
    var applying by remember { mutableStateOf(false) }

    val centered by remember {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(presets.indices) }
    }

    // Fetch every card's cover up front and in parallel. Loading only the centred one
    // meant the cards you scrolled to were still empty when you got there.
    LaunchedEffect(Unit) {
        coroutineScope {
            presets.forEach { preset ->
                launch {
                    val cover = repos.wallpapers.search(preset.wallpaperCategory).firstOrNull()
                    covers[preset.id] = cover
                    // Warm Coil's cache so the image is already decoded when it scrolls in.
                    cover?.let { c ->
                        imageLoader.enqueue(
                            ImageRequest.Builder(context).data(c.thumbnailUrl).build(),
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(EditorChrome)
                .clickable { navController.popBackStack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint = Color.White,
            )
        }

        LazyRow(
            state = listState,
            // Snapping is what makes the centred card actually land centred.
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier.align(Alignment.Center),
            contentPadding = PaddingValues(horizontal = 84.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            itemsIndexed(presets, key = { _, p -> p.id }) { index, preset ->
                PresetCard(
                    preset = preset,
                    cover = covers[preset.id],
                    focused = index == centered,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = presets[centered].name,
                color = Color.White,
                fontSize = 19.sp,
            )
            Text(
                text = presets[centered].description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(DiableAccent)
                    .clickable(enabled = !applying) {
                        applying = true
                        val preset = presets[centered]
                        scope.launch {
                            val applied = applyPreset(repos, preset)
                            applying = false
                            if (!applied) {
                                Toast.makeText(
                                    context,
                                    "Applied, but the wallpaper couldn't be downloaded",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            navController.popBackStack()
                        }
                    }
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (applying) {
                    CircularProgressIndicator(
                        color = Color(0xFF132569),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF132569))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Apply theme", color = Color(0xFF132569), fontSize = 17.sp)
            }
        }
    }
}

/** Commits every part of [preset]. Returns false if only the wallpaper failed. */
private suspend fun applyPreset(repos: DiableRepos, preset: ThemePreset): Boolean {
    val cover = repos.wallpapers.search(preset.wallpaperCategory).firstOrNull()
    val applied = cover?.let { repos.wallpapers.applyWallpaper(it) }
    repos.prefs.update {
        it.copy(
            appliedPresetId = preset.id,
            fontKey = preset.fontKey,
            clockStyleId = preset.clockStyleId,
            accentColorArgb = preset.accentArgb,
            iconStyleKey = preset.iconStyle.key,
            wallpaperUri = applied?.marker ?: "gradient:${preset.wallpaperCategory}",
            wallpaperCredit = cover?.let { c -> "${c.creator} · ${c.licenseName}" },
            wallpaperTopLuminance = applied?.topLuminance ?: -1f,
        )
    }
    return applied != null
}

@Composable
private fun PresetCard(
    preset: ThemePreset,
    cover: WebWallpaper?,
    focused: Boolean,
) {
    val accent = Color(preset.accentArgb)
    Box(
        modifier = Modifier
            .width(312.dp)
            .height(if (focused) 693.dp else 640.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(preset.previewColors)),
    ) {
        if (cover != null) {
            AsyncImage(
                model = cover.thumbnailUrl,
                contentDescription = preset.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f)),
        )
        Column(modifier = Modifier.padding(start = 26.dp, top = 130.dp)) {
            // The card previews the preset's own font and clock face, not the app's.
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontFamily = ThemeFont.fromKey(preset.fontKey).family,
                ),
            ) {
                ClockStylePreview(
                    styleId = preset.clockStyleId,
                    date = "Thu, Aug 30",
                    battery = "100%",
                )
                Spacer(modifier = Modifier.height(14.dp))
                // Real apps in the preset's own icon style and accent, so the card shows
                // exactly how applying it restyles the home screen.
                val sampleApps by LocalDiableRepos.current.apps.apps.collectAsState()
                sampleApps.take(4).forEach { app ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app = app, size = 26.dp, iconStyle = preset.iconStyle, accentColor = accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(app.name, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

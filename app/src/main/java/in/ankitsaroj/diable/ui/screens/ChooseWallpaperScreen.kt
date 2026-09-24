package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import `in`.ankitsaroj.diable.ui.screens.settings.applyGalleryWallpaper
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.WallpaperCatalog
import `in`.ankitsaroj.diable.data.WebWallpaper
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.components.ThemeScreenScaffold
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.launch

/**
 * Wallpaper picker. The top level is the category grid; tapping one loads real photos
 * for that category from Openverse. Everything shown is openly licensed, and the
 * creator/licence is displayed because most of these require attribution.
 */
@Composable
fun ChooseWallpaperScreen(
    navController: NavController,
    /** Where to go once a wallpaper is set; defaults to back. Create Theme continues on. */
    onApplied: (() -> Unit)? = null,
) {
    var openCategory by remember { mutableStateOf<String?>(null) }
    val finish: () -> Unit = onApplied ?: { navController.popBackStack() }
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val scope = rememberCoroutineScope()
    var applyingGallery by remember { mutableStateOf(false) }
    // The photo picker needs no storage permission.
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        applyingGallery = true
        scope.launch {
            val luminance = applyGalleryWallpaper(context, uri)
            applyingGallery = false
            if (luminance == null) {
                Toast.makeText(context, "Couldn't set that image", Toast.LENGTH_SHORT).show()
            } else {
                repos.prefs.update {
                    it.copy(
                        wallpaperUri = "gallery:${System.currentTimeMillis()}",
                        wallpaperCredit = null,
                        wallpaperTopLuminance = luminance,
                        appliedPresetId = null,
                    )
                }
                finish()
            }
        }
    }

    val category = openCategory
    if (category == null) {
        ThemeScreenScaffold(header = { PillHeader("Choose wallpaper") }) {
            CategoryGrid(
                onOpen = { openCategory = it },
                galleryBusy = applyingGallery,
                onGallery = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        }
    } else {
        // Back should step out of the category, not off the screen entirely.
        BackHandler { openCategory = null }
        ThemeScreenScaffold(header = { PillHeader(category) }) {
            WallpaperGallery(
                category = category,
                onApplied = finish,
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    onOpen: (String) -> Unit,
    galleryBusy: Boolean,
    onGallery: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DiableCard)
                    .clickable(enabled = !galleryBusy, onClick = onGallery)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (galleryBusy) {
                    CircularProgressIndicator(color = DiableAccentText, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = DiableAccentText)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text("Choose From Gallery", color = DiableText, fontSize = 17.sp)
            }
        }
        items(WallpaperCatalog.categories) { cat ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(cat.colors))
                    .clickable { onOpen(cat.name) },
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = cat.name,
                    color = DiableText,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }
}

@Composable
private fun WallpaperGallery(category: String, onApplied: () -> Unit) {
    val repos = LocalDiableRepos.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var results by remember(category) { mutableStateOf<List<WebWallpaper>?>(null) }
    var applying by remember { mutableStateOf<String?>(null) }
    // Tapping a thumbnail opens a full-screen preview; applying is a deliberate second step.
    var previewing by remember { mutableStateOf<WebWallpaper?>(null) }

    LaunchedEffect(category) {
        results = repos.wallpapers.search(category)
    }

    val items = results
    when {
        items == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = DiableAccentText)
        }

        items.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Couldn't load wallpapers. Check your connection and try again.",
                color = DiableTextMuted,
                fontSize = 15.sp,
            )
        }

        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { wallpaper ->
                WallpaperTile(
                    wallpaper = wallpaper,
                    applying = applying == wallpaper.id,
                    onClick = { previewing = wallpaper },
                )
            }
        }
    }

    previewing?.let { wallpaper ->
        WallpaperPreview(
            wallpaper = wallpaper,
            applying = applying == wallpaper.id,
            onDismiss = { if (applying == null) previewing = null },
            onApply = {
                if (applying != null) return@WallpaperPreview
                applying = wallpaper.id
                scope.launch {
                    val applied = repos.wallpapers.applyWallpaper(wallpaper)
                    applying = null
                    if (applied == null) {
                        Toast.makeText(
                            context,
                            "Couldn't download that wallpaper",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        repos.prefs.update {
                            it.copy(
                                wallpaperUri = applied.marker,
                                wallpaperCredit =
                                    "${wallpaper.creator} · ${wallpaper.licenseName}",
                                wallpaperTopLuminance = applied.topLuminance,
                            )
                        }
                        previewing = null
                        onApplied()
                    }
                }
            },
        )
    }
}

/** Full-screen look at a wallpaper before committing to it. */
@Composable
private fun WallpaperPreview(
    wallpaper: WebWallpaper,
    applying: Boolean,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    BackHandler(enabled = !applying, onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = !applying, onClick = onDismiss),
    ) {
        AsyncImage(
            model = wallpaper.fullUrl,
            contentDescription = wallpaper.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = wallpaper.title,
                color = DiableText,
                fontSize = 16.sp,
                maxLines = 1,
            )
            Text(
                text = "${wallpaper.creator} · ${wallpaper.licenseName}",
                color = DiableTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
            )
            Box(
                modifier = Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(DiableAccent)
                    .clickable(enabled = !applying, onClick = onApply)
                    .padding(horizontal = 36.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (applying) {
                    CircularProgressIndicator(
                        color = Color(0xFF132569),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text("Set wallpaper", color = Color(0xFF132569), fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun WallpaperTile(
    wallpaper: WebWallpaper,
    applying: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = wallpaper.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (applying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                }
            }
        }
        // CC-BY and friends require credit, so it rides along with every thumbnail.
        Text(
            text = "${wallpaper.creator} · ${wallpaper.licenseName}",
            color = DiableTextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}

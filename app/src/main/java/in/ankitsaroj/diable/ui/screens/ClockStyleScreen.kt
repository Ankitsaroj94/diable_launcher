package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.components.ThemeScreenScaffold
import `in`.ankitsaroj.diable.ui.home.CLOCK_STYLE_COUNT
import `in`.ankitsaroj.diable.ui.home.ClockStylePreview
import `in`.ankitsaroj.diable.ui.theme.ClockTileEnd
import `in`.ankitsaroj.diable.ui.theme.ClockTileStart
import kotlinx.coroutines.launch

/**
 * Diable's clock picker: a two-column grid of 342x205px tiles (228x137dp) with 8dp
 * gutters, each previewing a style against the same fixed sample.
 */
@Composable
fun ClockStyleScreen(navController: NavController) {
    val context = LocalContext.current
    val prefsRepo = LocalDiableRepos.current.prefs
    val prefs by prefsRepo.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()

    ThemeScreenScaffold(header = { PillHeader("Clock style") }) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items((0 until CLOCK_STYLE_COUNT).toList()) { id ->
                val selected = prefs.clockStyleId == id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(342f / 205f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ClockTileStart, ClockTileEnd),
                                start = Offset.Zero,
                                end = Offset.Infinite,
                            ),
                        )
                        .then(
                            if (selected) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = DiableText,
                                    shape = RoundedCornerShape(13.dp),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable {
                            scope.launch {
                                prefsRepo.update { it.copy(clockStyleId = id) }
                                navController.popBackStack()
                            }
                        },
                    // Diable anchors every preview to the tile's bottom-left corner.
                    contentAlignment = Alignment.BottomStart,
                ) {
                    ClockStylePreview(
                        styleId = id,
                        modifier = Modifier.padding(start = 12.dp, bottom = 10.dp),
                    )
                }
            }
        }
    }
}

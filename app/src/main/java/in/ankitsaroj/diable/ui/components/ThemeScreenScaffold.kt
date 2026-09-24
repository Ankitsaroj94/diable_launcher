package `in`.ankitsaroj.diable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import `in`.ankitsaroj.diable.ui.theme.DiableBg

@Composable
fun ThemeScreenScaffold(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DiableBg)
            .statusBarsPadding(),
    ) {
        header()
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

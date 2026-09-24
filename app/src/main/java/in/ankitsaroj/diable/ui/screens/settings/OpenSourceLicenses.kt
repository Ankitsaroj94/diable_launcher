package `in`.ankitsaroj.diable.ui.screens.settings

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import `in`.ankitsaroj.diable.ui.components.DiableBottomSheet
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

private data class OssLibrary(val name: String, val artifact: String, val license: String, val url: String)

private const val APACHE_2 = "Apache License 2.0"
private const val OFL = "SIL Open Font License 1.1"

/** Everything bundled in the APK, from app/build.gradle.kts plus the font files. */
private val Libraries = listOf(
    OssLibrary("AndroidX Core KTX", "androidx.core:core-ktx", APACHE_2, "https://developer.android.com/jetpack/androidx/releases/core"),
    OssLibrary("AndroidX Lifecycle", "androidx.lifecycle:lifecycle-runtime-ktx / -compose / -viewmodel-compose", APACHE_2, "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
    OssLibrary("AndroidX Activity Compose", "androidx.activity:activity-compose", APACHE_2, "https://developer.android.com/jetpack/androidx/releases/activity"),
    OssLibrary("Jetpack Compose", "androidx.compose.ui / material3 / material-icons-extended", APACHE_2, "https://developer.android.com/jetpack/androidx/releases/compose"),
    OssLibrary("Navigation Compose", "androidx.navigation:navigation-compose", APACHE_2, "https://developer.android.com/jetpack/androidx/releases/navigation"),
    OssLibrary("DataStore Preferences", "androidx.datastore:datastore-preferences", APACHE_2, "https://developer.android.com/jetpack/androidx/releases/datastore"),
    OssLibrary("Accompanist Drawable Painter", "com.google.accompanist:accompanist-drawablepainter", APACHE_2, "https://github.com/google/accompanist"),
    OssLibrary("Coil", "io.coil-kt:coil-compose", APACHE_2, "https://github.com/coil-kt/coil"),
    OssLibrary("OkHttp", "com.squareup.okhttp3:okhttp (via Coil)", APACHE_2, "https://github.com/square/okhttp"),
    OssLibrary("Kotlin Coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines", APACHE_2, "https://github.com/Kotlin/kotlinx.coroutines"),
    OssLibrary("Kotlin Standard Library", "org.jetbrains.kotlin:kotlin-stdlib", APACHE_2, "https://kotlinlang.org"),
    OssLibrary("Quicksand, Nunito, Space Grotesk, Playfair Display, Outfit", "Google Fonts", OFL, "https://openfontlicense.org"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Open source licenses",
                modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 12.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
            Libraries.forEach { lib ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(DiableCard, RoundedCornerShape(16.dp))
                        .clickable { openUrl(context, lib.url) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(lib.name, color = DiableText, fontSize = 16.sp)
                    Text(lib.artifact, color = DiableTextMuted, fontSize = 13.sp, lineHeight = 17.sp)
                    Text(lib.license, color = DiableAccentText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

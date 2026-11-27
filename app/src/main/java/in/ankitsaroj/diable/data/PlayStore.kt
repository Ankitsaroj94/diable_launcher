package `in`.ankitsaroj.diable.data

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens a Play Store search, falling back to the web store when the store app is missing. */
fun openPlayStoreSearch(context: Context, query: String) {
    val encoded = Uri.encode(query)
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$encoded&c=apps"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val web = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/search?q=$encoded&c=apps"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { context.startActivity(market) }.isFailure) {
        runCatching { context.startActivity(web) }
    }
}

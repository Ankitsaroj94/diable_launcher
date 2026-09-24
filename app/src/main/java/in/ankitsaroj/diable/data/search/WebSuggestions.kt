package `in`.ankitsaroj.diable.data.search

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Diable's web suggestion providers, in its display order. Every suggest endpoint here
 * answers in the OpenSearch shape `["query", ["s1", "s2", …]]`; providers without a public
 * one borrow DuckDuckGo's, which is keyless and privacy-preserving.
 */
enum class SearchEngine(
    val key: String,
    val label: String,
    private val searchUrl: String,
    private val suggestUrl: String?,
) {
    None("none", "No suggestions", "https://duckduckgo.com/?q=", null),
    Google(
        "google", "Google", "https://www.google.com/search?q=",
        "https://suggestqueries.google.com/complete/search?client=firefox&q=",
    ),
    DuckDuckGo("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=", DDG_SUGGEST),
    Bing("bing", "Bing", "https://www.bing.com/search?q=", "https://api.bing.com/osjson.aspx?query="),
    Brave(
        "brave", "Brave Search", "https://search.brave.com/search?q=",
        "https://search.brave.com/api/suggest?q=",
    ),
    Perplexity("perplexity", "Perplexity", "https://www.perplexity.ai/search?q=", DDG_SUGGEST),
    Startpage("startpage", "Startpage", "https://www.startpage.com/do/search?q=", DDG_SUGGEST),
    Kagi("kagi", "Kagi", "https://kagi.com/search?q=", DDG_SUGGEST),
    ;

    fun resultsUrl(query: String): String = searchUrl + encode(query)

    /** Up to [limit] suggestions for [query]; empty on any network or parse failure. */
    suspend fun suggestions(query: String, limit: Int = 4): List<String> {
        val base = suggestUrl ?: return emptyList()
        if (query.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val url = if (base == DDG_SUGGEST) {
                    "https://duckduckgo.com/ac/?q=${encode(query)}&type=list"
                } else {
                    base + encode(query)
                }
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    setRequestProperty("User-Agent", "Diable/1.0")
                }
                try {
                    if (conn.responseCode !in 200..299) return@withContext emptyList()
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val list = JSONArray(body).optJSONArray(1) ?: return@withContext emptyList()
                    List(list.length()) { list.optString(it) }
                        .filter { it.isNotBlank() && !it.equals(query.trim(), ignoreCase = true) }
                        .distinct()
                        .take(limit)
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.d("WebSuggestions", "Suggestions unavailable: ${e.message}")
                emptyList()
            }
        }
    }

    companion object {
        fun fromKey(key: String?): SearchEngine = entries.firstOrNull { it.key == key } ?: None

        private fun encode(q: String): String = URLEncoder.encode(q.trim(), "UTF-8")
    }
}

private const val DDG_SUGGEST = "ddg"

/** The contact's first phone number, for the call button on contact results. */
suspend fun primaryPhoneNumber(context: Context, contactId: Long): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                "${ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY} DESC",
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
    }

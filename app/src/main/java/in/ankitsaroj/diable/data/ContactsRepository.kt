package `in`.ankitsaroj.diable.data

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactInfo(
    val id: Long,
    val name: String,
    val lookupKey: String?,
)

class ContactsRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED

    suspend fun searchByName(query: String): List<ContactInfo> = withContext(Dispatchers.IO) {
        if (!hasPermission() || query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<ContactInfo>()
        val selection = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
        val args = arrayOf("%$query%")
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.LOOKUP_KEY,
            ),
            selection,
            args,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC LIMIT 20",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val lookupIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx) ?: continue
                val lookup = if (lookupIdx >= 0) cursor.getString(lookupIdx) else null
                results.add(ContactInfo(id = id, name = name, lookupKey = lookup))
            }
        }
        results
    }
}

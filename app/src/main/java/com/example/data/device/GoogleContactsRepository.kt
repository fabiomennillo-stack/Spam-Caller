package com.example.data.device

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.provider.ContactsContract
import com.example.data.remote.OnlineSpamLookupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val contactId: Long,
    val displayName: String,
    val photoUri: String?,
    val phoneNumbers: List<ContactPhone>,
    val emails: List<String>,
    val notes: String?,
    val organization: String?,
    val address: String?,
    val website: String?,
    val birthdayOrEvent: String?,
    val googleAccountName: String?
)

data class ContactPhone(
    val number: String,
    val normalizedNumber: String,
    val typeLabel: String
)

class GoogleContactsRepository(private val context: Context) {

    fun getGoogleAccounts(): List<Account> {
        return try {
            AccountManager.get(context).getAccountsByType("com.google").toList()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun requestGoogleContactsSync(): Boolean {
        val accounts = getGoogleAccounts()
        if (accounts.isEmpty()) return false
        val extras = Bundle().apply {
            putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        }
        accounts.forEach { account ->
            ContentResolver.requestSync(account, ContactsContract.AUTHORITY, extras)
        }
        return true
    }

    suspend fun loadContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contactsMap = LinkedHashMap<Long, MutableContactBuilder>()

        // 1. Base contacts (display name & photo URI)
        val baseProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            baseProjection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx) ?: "Senza nome"
                val photo = cursor.getString(photoIdx)
                contactsMap[id] = MutableContactBuilder(
                    contactId = id,
                    displayName = name,
                    photoUri = photo
                )
            }
        }

        if (contactsMap.isEmpty()) return@withContext emptyList()

        // 2. Google account attribution via RawContacts
        val rawProjection = arrayOf(
            ContactsContract.RawContacts.CONTACT_ID,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
            ContactsContract.RawContacts.ACCOUNT_NAME
        )
        resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            rawProjection,
            "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
            arrayOf("com.google"),
            null
        )?.use { cursor ->
            val cidIdx = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID)
            val accNameIdx = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)
            while (cursor.moveToNext()) {
                val cid = cursor.getLong(cidIdx)
                val accName = cursor.getString(accNameIdx)
                contactsMap[cid]?.googleAccountName = accName
            }
        }

        // 3. All Data rows: phones, emails, notes ("notizie salvate"), organization, address, website, events
        val dataProjection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA4
        )
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            dataProjection,
            null,
            null,
            null
        )?.use { cursor ->
            val cidIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val mimeIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val data2Idx = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val data4Idx = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA4)

            while (cursor.moveToNext()) {
                val cid = cursor.getLong(cidIdx)
                val builder = contactsMap[cid] ?: continue
                val mime = cursor.getString(mimeIdx) ?: continue
                val data1 = cursor.getString(data1Idx)?.trim().orEmpty()
                val data2 = cursor.getInt(data2Idx)
                val data4 = cursor.getString(data4Idx)?.trim().orEmpty()

                when (mime) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            val typeLabel = ContactsContract.CommonDataKinds.Phone
                                .getTypeLabel(context.resources, data2, "Altro")
                                .toString()
                            val normalized = OnlineSpamLookupService.normalizePhoneNumber(data1)
                            if (builder.phones.none { it.normalizedNumber == normalized }) {
                                builder.phones.add(ContactPhone(data1, normalized, typeLabel))
                            }
                        }
                    }

                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank() && data1 !in builder.emails) {
                            builder.emails.add(data1)
                        }
                    }

                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            builder.notes = listOfNotNull(builder.notes, data1)
                                .distinct()
                                .joinToString("\n")
                        }
                    }

                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        val orgText = listOf(data1, data4).filter { it.isNotBlank() }.joinToString(" • ")
                        if (orgText.isNotBlank()) {
                            builder.organization = orgText
                        }
                    }

                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            builder.address = data1
                        }
                    }

                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            builder.website = data1
                        }
                    }

                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            builder.birthdayOrEvent = data1
                        }
                    }
                }
            }
        }

        contactsMap.values
            .filter { it.phones.isNotEmpty() || it.emails.isNotEmpty() || !it.notes.isNullOrBlank() }
            .map { it.build() }
    }

    suspend fun findContactByNumber(rawNumber: String): DeviceContact? = withContext(Dispatchers.IO) {
        val target = OnlineSpamLookupService.normalizePhoneNumber(rawNumber)
        val suffix = target.takeLast(8)
        loadContacts().firstOrNull { contact ->
            contact.phoneNumbers.any { phone ->
                phone.normalizedNumber == target ||
                    (suffix.length >= 7 && phone.normalizedNumber.endsWith(suffix))
            }
        }
    }

    private class MutableContactBuilder(
        val contactId: Long,
        val displayName: String,
        val photoUri: String?
    ) {
        val phones = mutableListOf<ContactPhone>()
        val emails = mutableListOf<String>()
        var notes: String? = null
        var organization: String? = null
        var address: String? = null
        var website: String? = null
        var birthdayOrEvent: String? = null
        var googleAccountName: String? = null

        fun build() = DeviceContact(
            contactId = contactId,
            displayName = displayName,
            photoUri = photoUri,
            phoneNumbers = phones,
            emails = emails,
            notes = notes,
            organization = organization,
            address = address,
            website = website,
            birthdayOrEvent = birthdayOrEvent,
            googleAccountName = googleAccountName
        )
    }
}

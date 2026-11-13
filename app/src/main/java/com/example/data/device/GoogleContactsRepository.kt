package com.example.data.device

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.remote.OnlineSpamLookupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val contactId: Long,
    val lookupKey: String? = null,
    val displayName: String,
    val photoUri: String?,
    val isStarred: Boolean = false,
    val nickname: String? = null,
    val phoneNumbers: List<ContactPhone>,
    val emails: List<String>,
    val emailDetails: List<ContactLabeledValue> = emptyList(),
    val notes: String?,
    val organization: String?,
    val address: String?,
    val postalAddresses: List<ContactLabeledValue> = emptyList(),
    val website: String?,
    val websites: List<String> = emptyList(),
    val birthdayOrEvent: String?,
    val events: List<ContactLabeledValue> = emptyList(),
    val imHandles: List<String> = emptyList(),
    val googleAccountName: String?,
    val whatsappCallDataId: Long? = null,
    val whatsappVideoCallDataId: Long? = null,
    val whatsappProfileDataId: Long? = null
)

data class ContactPhone(
    val number: String,
    val normalizedNumber: String,
    val typeLabel: String
)

data class ContactLabeledValue(
    val value: String,
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

    suspend fun setSystemContactStarred(contactId: Long, starred: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val hasWrite = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasWrite) return@withContext false

            return@withContext try {
                val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
                val values = ContentValues().apply {
                    put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
                }
                context.contentResolver.update(uri, values, null, null) > 0
            } catch (_: Exception) {
                false
            }
        }

    suspend fun loadContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contactsMap = LinkedHashMap<Long, MutableContactBuilder>()

        // 1. Base contacts (display name, photo URI, starred favorite flag, lookup key)
        val baseProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.STARRED,
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
            val lookupIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
            val starredIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val lookupKey = cursor.getString(lookupIdx)
                val name = cursor.getString(nameIdx) ?: "Senza nome"
                val photo = cursor.getString(photoIdx)
                val isStarred = cursor.getInt(starredIdx) == 1
                contactsMap[id] = MutableContactBuilder(
                    contactId = id,
                    lookupKey = lookupKey,
                    displayName = name,
                    photoUri = photo,
                    isStarred = isStarred
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

        // 3. All Data rows: phones, emails, notes, organization, addresses, websites, events, nickname, IM, WhatsApp links
        val dataProjection = arrayOf(
            ContactsContract.Data._ID,
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA4,
            ContactsContract.Data.DATA5
        )
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            dataProjection,
            null,
            null,
            null
        )?.use { cursor ->
            val dataIdIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID)
            val cidIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val mimeIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val data2Idx = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val data4Idx = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA4)
            val data5Idx = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA5)

            while (cursor.moveToNext()) {
                val dataId = cursor.getLong(dataIdIdx)
                val cid = cursor.getLong(cidIdx)
                val builder = contactsMap[cid] ?: continue
                val mime = cursor.getString(mimeIdx) ?: continue
                val data1 = cursor.getString(data1Idx)?.trim().orEmpty()
                val data2 = cursor.getInt(data2Idx)
                val data4 = cursor.getString(data4Idx)?.trim().orEmpty()
                val data5 = cursor.getString(data5Idx)?.trim().orEmpty()

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
                        if (data1.isNotBlank()) {
                            if (data1 !in builder.emails) {
                                builder.emails.add(data1)
                            }
                            if (builder.emailDetails.none { it.value.equals(data1, ignoreCase = true) }) {
                                val typeLabel = ContactsContract.CommonDataKinds.Email
                                    .getTypeLabel(context.resources, data2, "Email")
                                    .toString()
                                builder.emailDetails.add(ContactLabeledValue(data1, typeLabel))
                            }
                        }
                    }

                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            builder.notes = listOfNotNull(builder.notes, data1)
                                .distinct()
                                .joinToString("\n")
                        }
                    }

                    ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            builder.nickname = data1
                        }
                    }

                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        val orgText = listOf(data1, data4, data5)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                        if (orgText.isNotBlank()) {
                            builder.organization = orgText
                        }
                    }

                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            if (builder.address == null) builder.address = data1
                            if (builder.postalAddresses.none { it.value == data1 }) {
                                val typeLabel = ContactsContract.CommonDataKinds.StructuredPostal
                                    .getTypeLabel(context.resources, data2, "Indirizzo")
                                    .toString()
                                builder.postalAddresses.add(ContactLabeledValue(data1, typeLabel))
                            }
                        }
                    }

                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            if (builder.website == null) builder.website = data1
                            if (data1 !in builder.websites) {
                                builder.websites.add(data1)
                            }
                        }
                    }

                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            if (builder.birthdayOrEvent == null) builder.birthdayOrEvent = data1
                            val typeLabel = ContactsContract.CommonDataKinds.Event
                                .getTypeLabel(context.resources, data2, "Ricorrenza")
                                .toString()
                            if (builder.events.none { it.value == data1 && it.typeLabel == typeLabel }) {
                                builder.events.add(ContactLabeledValue(data1, typeLabel))
                            }
                        }
                    }

                    ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank() && data1 !in builder.imHandles) {
                            builder.imHandles.add(data1)
                        }
                    }

                    MIME_WHATSAPP_VOIP_CALL -> {
                        if (builder.whatsappCallDataId == null) {
                            builder.whatsappCallDataId = dataId
                        }
                    }

                    MIME_WHATSAPP_VIDEO_CALL -> {
                        if (builder.whatsappVideoCallDataId == null) {
                            builder.whatsappVideoCallDataId = dataId
                        }
                    }

                    MIME_WHATSAPP_PROFILE -> {
                        if (builder.whatsappProfileDataId == null) {
                            builder.whatsappProfileDataId = dataId
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
        val lookupKey: String?,
        val displayName: String,
        val photoUri: String?,
        val isStarred: Boolean
    ) {
        var nickname: String? = null
        val phones = mutableListOf<ContactPhone>()
        val emails = mutableListOf<String>()
        val emailDetails = mutableListOf<ContactLabeledValue>()
        var notes: String? = null
        var organization: String? = null
        var address: String? = null
        val postalAddresses = mutableListOf<ContactLabeledValue>()
        var website: String? = null
        val websites = mutableListOf<String>()
        var birthdayOrEvent: String? = null
        val events = mutableListOf<ContactLabeledValue>()
        val imHandles = mutableListOf<String>()
        var googleAccountName: String? = null
        var whatsappCallDataId: Long? = null
        var whatsappVideoCallDataId: Long? = null
        var whatsappProfileDataId: Long? = null

        fun build() = DeviceContact(
            contactId = contactId,
            lookupKey = lookupKey,
            displayName = displayName,
            photoUri = photoUri,
            isStarred = isStarred,
            nickname = nickname,
            phoneNumbers = phones,
            emails = emails,
            emailDetails = emailDetails,
            notes = notes,
            organization = organization,
            address = address,
            postalAddresses = postalAddresses,
            website = website,
            websites = websites,
            birthdayOrEvent = birthdayOrEvent,
            events = events,
            imHandles = imHandles,
            googleAccountName = googleAccountName,
            whatsappCallDataId = whatsappCallDataId,
            whatsappVideoCallDataId = whatsappVideoCallDataId,
            whatsappProfileDataId = whatsappProfileDataId
        )
    }

    companion object {
        const val MIME_WHATSAPP_VOIP_CALL = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
        const val MIME_WHATSAPP_VIDEO_CALL = "vnd.android.cursor.item/vnd.com.whatsapp.video.call"
        const val MIME_WHATSAPP_PROFILE = "vnd.android.cursor.item/vnd.com.whatsapp.profile"
    }
}

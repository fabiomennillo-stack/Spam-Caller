package com.example.data.device

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

data class SystemRoleStatus(
    val isDefaultDialer: Boolean,
    val isCallScreeningApp: Boolean,
    val canRequestDialerRole: Boolean,
    val canRequestCallScreeningRole: Boolean
)

object SystemRoleHelper {

    fun getRoleStatus(context: Context): SystemRoleStatus {
        val telecomManager = context.getSystemService(TelecomManager::class.java)
        val isDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true ||
                telecomManager?.defaultDialerPackage == context.packageName
        } else {
            telecomManager?.defaultDialerPackage == context.packageName
        }

        val isScreening = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }

        val canDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
                ?.isRoleAvailable(RoleManager.ROLE_DIALER) == true
        } else {
            true
        }

        val canScreening = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
                ?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }

        return SystemRoleStatus(
            isDefaultDialer = isDialer,
            isCallScreeningApp = isScreening,
            canRequestDialerRole = canDialer,
            canRequestCallScreeningRole = canScreening
        )
    }

    fun createDefaultDialerIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else null
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    context.packageName
                )
            }
        }
    }

    fun createCallScreeningIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            } else null
        } else {
            null
        }
    }

    fun placePhoneCall(context: Context, rawNumber: String) {
        val cleaned = rawNumber.trim()
        if (cleaned.isBlank()) return
        val telUri = Uri.fromParts("tel", cleaned, null)
        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val telecomManager = context.getSystemService(TelecomManager::class.java)
        val isDefaultDialer = telecomManager?.defaultDialerPackage == context.packageName

        if (isDefaultDialer && hasCallPerm && telecomManager != null) {
            try {
                telecomManager.placeCall(telUri, Bundle())
                return
            } catch (_: SecurityException) {
            }
        }

        val action = if (hasCallPerm) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val intent = Intent(action, telUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Triggers a WhatsApp voice call using the synced ContactsContract WhatsApp VoIP Data row if available,
     * falling back to opening the contact directly in WhatsApp via wa.me.
     */
    fun openWhatsAppCall(
        context: Context,
        rawNumber: String,
        whatsappCallDataId: Long? = null
    ): Boolean {
        if (whatsappCallDataId != null) {
            for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b", null)) {
                val voipIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, whatsappCallDataId.toString()),
                        GoogleContactsRepository.MIME_WHATSAPP_VOIP_CALL
                    )
                    if (pkg != null) setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(context, voipIntent)) return true
            }
        }
        return openWhatsAppChat(context, rawNumber, null)
    }

    /**
     * Opens the WhatsApp chat/contact screen for the given number or synced WhatsApp profile row.
     */
    fun openWhatsAppChat(
        context: Context,
        rawNumber: String,
        whatsappProfileDataId: Long? = null
    ): Boolean {
        if (whatsappProfileDataId != null) {
            for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b", null)) {
                val profileIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, whatsappProfileDataId.toString()),
                        GoogleContactsRepository.MIME_WHATSAPP_PROFILE
                    )
                    if (pkg != null) setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(context, profileIntent)) return true
            }
        }

        val waDigits = normalizeForWhatsApp(rawNumber)
        if (waDigits.isBlank()) return false
        val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$waDigits")

        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b", null)) {
            val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
                if (pkg != null) setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (tryStartActivity(context, intent)) return true
        }
        return false
    }

    fun sendEmailToContact(context: Context, email: String, subject: String? = null): Boolean {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) return false
        val uriBuilder = StringBuilder("mailto:").append(Uri.encode(cleanEmail))
        if (!subject.isNullOrBlank()) {
            uriBuilder.append("?subject=").append(Uri.encode(subject))
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(uriBuilder.toString())).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return tryStartActivity(context, intent)
    }

    fun openMapAddress(context: Context, address: String): Boolean {
        val clean = address.trim()
        if (clean.isBlank()) return false
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(clean)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return tryStartActivity(context, intent)
    }

    fun openWebsiteUrl(context: Context, rawUrl: String): Boolean {
        val clean = rawUrl.trim()
        if (clean.isBlank()) return false
        val normalized = if (clean.startsWith("http://") || clean.startsWith("https://")) {
            clean
        } else {
            "https://$clean"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return tryStartActivity(context, intent)
    }

    fun openSystemContactEditor(context: Context, contactId: Long, lookupKey: String?): Boolean {
        val uri = if (!lookupKey.isNullOrBlank()) {
            ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
        } else {
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString())
        } ?: return false

        val intent = Intent(Intent.ACTION_EDIT, uri).apply {
            putExtra("finishActivityOnSaveCompleted", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return tryStartActivity(context, intent)
    }

    private fun normalizeForWhatsApp(rawNumber: String): String {
        val trimmed = rawNumber.trim()
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.isBlank()) return ""
        return when {
            hasPlus -> digitsOnly
            digitsOnly.startsWith("00") && digitsOnly.length > 4 -> digitsOnly.drop(2)
            // Italian mobile/landline without country code (e.g. 347... or 02...)
            (digitsOnly.startsWith("3") && digitsOnly.length in 9..10) ||
                (digitsOnly.startsWith("0") && digitsOnly.length in 8..11) -> "39$digitsOnly"
            else -> digitsOnly
        }
    }

    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}

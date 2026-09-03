package com.example.message_app.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.PhoneLookup
import androidx.core.content.ContextCompat

object ContactResolver {
    fun name(context: Context, number: String?): String? {
        if (number.isNullOrBlank() || ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return null
        return runCatching {
            val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(
                uri, arrayOf(PhoneLookup.DISPLAY_NAME), null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() }
                else null
            }
        }.getOrNull() // Revoked permissions or provider failure must not drop the call.
    }
}

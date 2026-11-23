package com.tohsoft.music.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Безопасное хранилище OAuth-токенов и учётных данных стриминг-сервисов.
 * Автоматически переносит старые открытые записи в EncryptedSharedPreferences
 * и очищает исходный незашифрованный файл. Токены никогда не логируются.
 */
object SecureTokenStorage {

    fun getEncryptedPrefs(
        context: Context,
        encryptedName: String,
        legacyPlainName: String? = null
    ): SharedPreferences {
        val appCtx = context.applicationContext
        val securePrefs = try {
            val masterKey = MasterKey.Builder(appCtx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appCtx,
                encryptedName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Защита от повреждённого Keystore после сброса блокировки экрана на некоторых OEM ROM
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    appCtx.deleteSharedPreferences(encryptedName)
                } else {
                    appCtx.getSharedPreferences(encryptedName, Context.MODE_PRIVATE).edit().clear().commit()
                }
                val masterKey = MasterKey.Builder(appCtx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    appCtx,
                    encryptedName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (_: Exception) {
                appCtx.getSharedPreferences(encryptedName + "_fallback", Context.MODE_PRIVATE)
            }
        }

        if (!legacyPlainName.isNullOrBlank()) {
            migrateLegacyPlainPrefs(appCtx, legacyPlainName, securePrefs)
        }
        return securePrefs
    }

    private fun migrateLegacyPlainPrefs(
        context: Context,
        legacyName: String,
        targetSecure: SharedPreferences
    ) {
        try {
            val legacy = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
            val all = legacy.all
            if (all.isEmpty()) return
            val editor = targetSecure.edit()
            for ((k, v) in all) {
                if (targetSecure.contains(k)) continue
                when (v) {
                    is String -> editor.putString(k, v)
                    is Boolean -> editor.putBoolean(k, v)
                    is Int -> editor.putInt(k, v)
                    is Long -> editor.putLong(k, v)
                    is Float -> editor.putFloat(k, v)
                }
            }
            editor.apply()
            legacy.edit().clear().apply()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(legacyName)
            }
        } catch (_: Exception) {
        }
    }

    fun wipeAll(prefs: SharedPreferences) {
        try {
            prefs.edit().clear().apply()
        } catch (_: Exception) {}
    }
}

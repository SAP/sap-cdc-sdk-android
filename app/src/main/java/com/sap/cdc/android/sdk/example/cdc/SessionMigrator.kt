package com.sap.cdc.android.sdk.example.cdc

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.sap.cdc.android.sdk.auth.AuthenticationService
import com.sap.cdc.android.sdk.auth.session.SessionService
import java.math.BigInteger
import java.security.KeyStore
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * User migrator class to initiate session migration for previous versions of the CDC
 * SDK from version 6+.
 * The migrator will decode a secured session that was created by previous SDK (v6+) and
 * re-secure it using current SDK standards.
 *
 * Use this class in any "init" function of your CDC SDK wrapper class to ensure migration is
 * preformed before any other SDK actions.
 */
class SessionMigrator(private val context: Context) {

    private var keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private var preferences: SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    init {
        keyStore.load(null)
    }

    companion object {
        const val ALIAS = "GS_ALIAS_V2"
        const val PREF_FILE = "GSLIB"
        const val PREF_SESSION_ENTRY = "GS_PREFS"
        const val PREF_SESSION_IV_SPEC = "IV_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"

        const val LOG_TAG = "SessionMigrator"
    }

    /**
     * Try to migrate an existing session created from an old version of the CDC SDK.
     */
    fun tryMigrateSession(
        authenticationService: AuthenticationService,
        success: () -> Unit,
        failure: () -> Unit
    ) {
        if (!sessionAvailableForMigration()) {
            failure()
        }
        getSession(
            success = { session ->
                if (session == null) {
                    failure()
                    return@getSession
                }
                // Set the session.
                authenticationService.session().setSession(session!!)
                success()
            },
            error = { message ->
                Log.e(SessionService.LOG_TAG, message)
                failure()
            }
        )

    }

    /**
     * Check if the Android Keystore has an old session alias.
     */
    private fun sessionAvailableForMigration(): Boolean = keyStore.containsAlias(ALIAS)

    /**
     * Get session keystore key for decryption.
     */
    private fun getKeyV2(): SecretKey? {
        keyStore.load(null)
        if (keyStore.containsAlias(ALIAS)) {
            // Alias available. Key generated.
            val secretKeyEntry = keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
            return secretKeyEntry.secretKey
        }
        return null
    }

    /**
     * Clear old SDK keystore alias.
     */
    private fun clearKeyV2() {
        keyStore.deleteEntry(ALIAS)
    }

    /**
     * Clear old SDK shared preferences entries.
     */
    private fun clearPrefsV2() {
        preferences.edit()
            .remove(PREF_SESSION_ENTRY)
            .remove(PREF_SESSION_IV_SPEC).apply()
        context.deleteSharedPreferences(PREF_FILE)
    }

    /**
     * Try to fetch available session.
     */
    private fun getSession(
        success: (String?) -> Unit,
        error: (String) -> Unit
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)

        val encryptedSession: String? = preferences.getString(PREF_SESSION_ENTRY, null)
        if (encryptedSession == null) {
            Log.e(LOG_TAG, "Session not available for migration")
            error("$LOG_TAG: Session not available")
            return
        }

        val ivSpecString: String? =
            preferences.getString(PREF_SESSION_IV_SPEC, null)
        if (ivSpecString == null) {
            Log.e(LOG_TAG, "Session not migrated to GCM. Cannot be migrated")
            error("$LOG_TAG: Session not migrated to GCM. Cannot be migrated")
            return
        }

        // Decrypt session.
        val ivSpec = GCMParameterSpec(128, Base64.decode(ivSpecString, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, getKeyV2(), ivSpec)
        val encPLBytes = encryptedSession.stringToBytes()
        val sessionString = String(cipher.doFinal(encPLBytes))

        // Delete v6 keystore entry.
        clearKeyV2()
        // Delete v6 preferences records.
        clearPrefsV2()

        success(sessionString)
    }
}

/**
 * Migrator String helper extension.
 */
fun String.stringToBytes(): ByteArray {
    val b2 = BigInteger(this, 36).toByteArray()
    return Arrays.copyOfRange(b2, 1, b2.size)
}
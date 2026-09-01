package com.thebifel.bubnilka.storage

import android.content.Context
import android.content.SharedPreferences

interface ConsentStore {
    fun readAccepted(): Boolean
    fun readDisclosureVersion(): Int
    fun write(accepted: Boolean, disclosureVersion: Int)
}

class SharedPreferencesConsentStore(
    private val preferences: SharedPreferences,
) : ConsentStore {
    override fun readAccepted(): Boolean = preferences.getBoolean(KEY_ACCEPTED, false)

    override fun readDisclosureVersion(): Int = preferences.getInt(KEY_VERSION, 0)

    override fun write(accepted: Boolean, disclosureVersion: Int) {
        preferences.edit()
            .putBoolean(KEY_ACCEPTED, accepted)
            .putInt(KEY_VERSION, disclosureVersion)
            .apply()
    }

    private companion object {
        const val KEY_ACCEPTED = "cloud_processing_accepted"
        const val KEY_VERSION = "accepted_disclosure_version"
    }
}

class ConsentRepository(
    private val store: ConsentStore,
) {
    fun isAccepted(): Boolean =
        store.readAccepted() && store.readDisclosureVersion() == CURRENT_DISCLOSURE_VERSION

    fun accept() {
        store.write(accepted = true, disclosureVersion = CURRENT_DISCLOSURE_VERSION)
    }

    fun revoke() {
        store.write(accepted = false, disclosureVersion = 0)
    }

    companion object {
        const val CURRENT_DISCLOSURE_VERSION: Int = 1
        private const val PREFERENCES_NAME = "bubnilka_privacy"

        fun from(context: Context): ConsentRepository = ConsentRepository(
            SharedPreferencesConsentStore(
                context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
            ),
        )
    }
}

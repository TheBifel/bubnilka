package com.thebifel.bubnilka.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentRepositoryTest {
    @Test
    fun consentIsDeniedByDefault() {
        assertFalse(ConsentRepository(FakeConsentStore()).isAccepted())
    }

    @Test
    fun acceptPersistsCurrentDisclosureVersion() {
        val store = FakeConsentStore()
        val repository = ConsentRepository(store)

        repository.accept()

        assertTrue(repository.isAccepted())
        assertTrue(store.accepted)
        assertTrue(store.version == ConsentRepository.CURRENT_DISCLOSURE_VERSION)
    }

    @Test
    fun oldDisclosureVersionRequiresFreshConsent() {
        val store = FakeConsentStore(accepted = true, version = ConsentRepository.CURRENT_DISCLOSURE_VERSION - 1)

        assertFalse(ConsentRepository(store).isAccepted())
    }

    @Test
    fun revokeImmediatelyDisablesCloudProcessing() {
        val store = FakeConsentStore()
        val repository = ConsentRepository(store)
        repository.accept()

        repository.revoke()

        assertFalse(repository.isAccepted())
        assertFalse(store.accepted)
    }

    private class FakeConsentStore(
        var accepted: Boolean = false,
        var version: Int = 0,
    ) : ConsentStore {
        override fun readAccepted(): Boolean = accepted
        override fun readDisclosureVersion(): Int = version
        override fun write(accepted: Boolean, disclosureVersion: Int) {
            this.accepted = accepted
            version = disclosureVersion
        }
    }
}

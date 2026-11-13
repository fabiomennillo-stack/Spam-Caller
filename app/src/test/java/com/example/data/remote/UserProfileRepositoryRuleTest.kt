package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfileInitialized_authenticatedUser_createsAndObservesProfile() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = UserProfileRepository(firestore)

        val initResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureProfileInitialized(
                displayName = "Alice Spamcaller",
                email = ALICE_EMAIL,
                photoUrl = "https://example.com/alice.png"
            )
        }
        assertTrue(initResult.isSuccess)
        val profile = initResult.getOrThrow()
        assertEquals(uid, profile.userId)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first { it != null }
        }
        assertNotNull(observed)
        assertEquals("Alice Spamcaller", observed?.displayName)
    }

    @Test
    fun updateSettings_authenticatedOwner_updatesFieldsSuccessfully() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = UserProfileRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureProfileInitialized("Alice", ALICE_EMAIL, "").getOrThrow()
        }

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateSettings(
                autoBlockEnabled = false,
                spamThreshold = 85,
                blockHiddenNumbers = true,
                lastPhoneBackupLabel = "spamcaller_backup_2026.json"
            )
        }
        assertTrue(updateResult.isSuccess)

        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first { it?.spamThreshold == 85L }
        }
        assertEquals(false, updated?.autoBlockEnabled)
        assertEquals(true, updated?.blockHiddenNumbers)
    }

    @Test
    fun observeProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = UserProfileRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureProfileInitialized("Alice", ALICE_EMAIL, "").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = UserProfileRepository(firestore)
        var captured: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeProfile(aliceUid).first()
            }
        } catch (t: Throwable) {
            captured = t
        }
        val firestoreEx = captured?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException, got $captured", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeProfile_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = UserProfileRepository(firestore)

        var captured: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProfile("alice_uid_placeholder").first()
            }
        } catch (t: Throwable) {
            captured = t
        }
        val firestoreEx = captured?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException, got $captured", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@spamcaller.test"
        const val BOB_EMAIL = "bob@spamcaller.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}

package com.mocca.app.data.repository

import com.mocca.app.data.local.FakeLocalCache
import com.mocca.app.domain.model.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @Test
    fun getLastSessionIdReturnsNullWhenNotSet() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        assertNull(repo.getLastSessionId())
    }

    @Test
    fun saveAndRetrieveLastSessionId() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        repo.saveLastSessionId("ses_123")
        assertEquals("ses_123", repo.getLastSessionId())
    }

    @Test
    fun saveNullSessionIdDeletesSetting() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        repo.saveLastSessionId("ses_123")
        repo.saveLastSessionId(null)
        assertNull(repo.getLastSessionId())
    }

    @Test
    fun saveBlankSessionIdDeletesSetting() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        repo.saveLastSessionId("ses_123")
        repo.saveLastSessionId("   ")
        assertNull(repo.getLastSessionId())
    }

    @Test
    fun getUserPreferencesReturnsDefaultsWhenUnset() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        val prefs = repo.getUserPreferences()
        assertEquals(UserPreferences.DEFAULT, prefs)
    }

    @Test
    fun saveAndRetrieveUserPreferences() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        repo.setShowTokenCounts(false)
        repo.setShowTimestamps(false)
        repo.setCodeFontFamily("fira_code")
        repo.setNotifyPermissions(false)
        repo.setNotifySessionComplete(false)
        repo.setNotifyConnectionLost(false)
        repo.setAutoUpdateCheckInterval(30)
        repo.saveLastSessionId("ses_456")

        val prefs = repo.getUserPreferences()
        assertEquals("ses_456", prefs.lastSessionId)
        assertEquals(false, prefs.showTokenCounts)
        assertEquals(false, prefs.showTimestamps)
        assertEquals("fira_code", prefs.codeFontFamily)
        assertEquals(false, prefs.notifyPermissions)
        assertEquals(false, prefs.notifySessionComplete)
        assertEquals(false, prefs.notifyConnectionLost)
        assertEquals(30, prefs.autoUpdateCheckIntervalMinutes)
    }

    @Test
    fun githubTokenRoundTripsThroughEncryption() = runTest {
        val cache = FakeLocalCache()
        val storage = ReversingFakeSecureTokenStorage()
        val repo = SettingsRepository(cache, storage)
        repo.saveGitHubToken("ghp_secret_token")
        // Stored value should be encrypted, not plaintext
        val stored = cache.settings[SettingsRepository.KEY_GITHUB_TOKEN]
        assertTrue(stored != null && stored != "ghp_secret_token")
        assertEquals("ghp_secret_token", repo.getGitHubToken())
    }

    @Test
    fun blankGithubTokenDeletesSetting() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, ReversingFakeSecureTokenStorage())
        repo.saveGitHubToken("ghp_secret")
        repo.saveGitHubToken("")
        assertNull(cache.settings[SettingsRepository.KEY_GITHUB_TOKEN])
    }

    @Test
    fun clearGithubTokenRemovesSetting() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, ReversingFakeSecureTokenStorage())
        repo.saveGitHubToken("ghp_secret")
        repo.clearGitHubToken()
        assertNull(cache.settings[SettingsRepository.KEY_GITHUB_TOKEN])
    }

    @Test
    fun plaintextGithubTokenMigratesToEncryptedOnRead() = runTest {
        val cache = FakeLocalCache()
        val storage = ReversingFakeSecureTokenStorage()
        // Simulate a legacy plaintext token in the cache
        cache.settings[SettingsRepository.KEY_GITHUB_TOKEN] = "ghp_legacy_plaintext"
        val repo = SettingsRepository(cache, storage)

        // First read should return the plaintext and migrate to encrypted
        val token = repo.getGitHubToken()
        assertEquals("ghp_legacy_plaintext", token)

        // The stored value should now be encrypted
        val stored = cache.settings[SettingsRepository.KEY_GITHUB_TOKEN]
        assertTrue(stored != null && storage.isEncrypted(stored))
    }

    @Test
    fun activeDownloadIdDefaultsToMinusOne() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        assertEquals(-1L, repo.getActiveDownloadId())
    }

    @Test
    fun saveAndRetrieveActiveDownloadId() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        repo.setActiveDownloadId(42L)
        assertEquals(42L, repo.getActiveDownloadId())
    }

    @Test
    fun settingActiveDownloadIdToMinusOneDeletesIt() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        repo.setActiveDownloadId(99L)
        repo.setActiveDownloadId(-1L)
        assertNull(cache.settings[SettingsRepository.KEY_ACTIVE_DOWNLOAD_ID])
    }

    @Test
    fun downloadedVersionRoundTrips() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        repo.setDownloadedVersion("1.0.1-build.42")
        assertEquals("1.0.1-build.42", repo.getDownloadedVersion())
    }

    @Test
    fun setNullDownloadedVersionDeletesIt() = runTest {
        val cache = FakeLocalCache()
        val repo = SettingsRepository(cache, NoOpFakeSecureTokenStorage)
        repo.setDownloadedVersion("1.0.1")
        repo.setDownloadedVersion(null)
        assertNull(repo.getDownloadedVersion())
    }
}

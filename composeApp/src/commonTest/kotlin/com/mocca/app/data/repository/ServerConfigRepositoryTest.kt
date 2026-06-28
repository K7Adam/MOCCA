package com.mocca.app.data.repository

import com.mocca.app.data.local.FakeLocalCache
import com.mocca.app.domain.model.ServerConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ServerConfigRepositoryTest {

    private fun makeConfig(
        id: String = "srv_1",
        name: String = "Test Server",
        password: String = "secret123"
    ) = ServerConfig(
        id = id,
        name = name,
        host = "10.0.2.2",
        port = 3000,
        username = "admin",
        password = password,
        isActive = true,
        useHttps = false
    )

    @Test
    fun noActiveServerOnFirstRun() {
        val cache = FakeLocalCache()
        val repo = ServerConfigRepository(cache, null)
        assertNull(repo.getActiveServerConfig())
        assertTrue(repo.isLoaded.value)
    }

    @Test
    fun saveActiveServerPersistsToCache() = runTest {
        val cache = FakeLocalCache()
        val repo = ServerConfigRepository(cache, null)
        val config = makeConfig()
        repo.saveActiveServer(config)
        assertEquals(config, repo.getActiveServerConfig())
        assertEquals(config, cache.getActiveServerConfig())
    }

    @Test
    fun saveActiveServerEncryptsPasswordWhenSecureStorageAvailable() = runTest {
        val cache = FakeLocalCache()
        val storage = ReversingFakeSecureTokenStorage()
        val repo = ServerConfigRepository(cache, storage)
        val config = makeConfig(password = "my_secret")
        repo.saveActiveServer(config)

        // The in-memory active server should have the plaintext password
        assertEquals("my_secret", repo.getActiveServerConfig()!!.password)

        // The persisted config should have the encrypted password
        val persisted = cache.getActiveServerConfig()
        assertTrue(persisted != null && storage.isEncrypted(persisted.password))
        assertFalse(persisted!!.password == "my_secret")
    }

    @Test
    fun saveActiveServerWithEmptyPasswordDoesNotEncrypt() = runTest {
        val cache = FakeLocalCache()
        val storage = ReversingFakeSecureTokenStorage()
        val repo = ServerConfigRepository(cache, storage)
        val config = makeConfig(password = "")
        repo.saveActiveServer(config)

        val persisted = cache.getActiveServerConfig()
        assertEquals("", persisted!!.password)
    }

    @Test
    fun saveActiveServerReplacesPreviousConfig() = runTest {
        val cache = FakeLocalCache()
        val repo = ServerConfigRepository(cache, null)
        repo.saveActiveServer(makeConfig(id = "srv_1", name = "First"))
        repo.saveActiveServer(makeConfig(id = "srv_2", name = "Second"))

        assertEquals("Second", repo.getActiveServerConfig()!!.name)
        assertEquals("srv_2", cache.getActiveServerConfig()!!.id)
    }

    @Test
    fun loadedServerFromCacheHasDecryptedPassword() = runTest {
        val cache = FakeLocalCache()
        val storage = ReversingFakeSecureTokenStorage()
        // Pre-populate cache with an encrypted password
        val encryptedPassword = storage.encrypt("decrypted_secret")
        cache.insertServerConfig(makeConfig(password = encryptedPassword))
        cache.setActiveServerConfig("srv_1")

        val repo = ServerConfigRepository(cache, storage)
        val active = repo.getActiveServerConfig()
        assertTrue(active != null)
        assertEquals("decrypted_secret", active!!.password)
    }

    @Test
    fun plaintextPasswordFromCacheMigratesToEncrypted() = runTest {
        val cache = FakeLocalCache()
        val storage = ReversingFakeSecureTokenStorage()
        // Pre-populate cache with a plaintext password (legacy)
        cache.insertServerConfig(makeConfig(password = "legacy_plaintext"))
        cache.setActiveServerConfig("srv_1")

        val repo = ServerConfigRepository(cache, storage)
        // Reading the active server should return the plaintext password
        assertEquals("legacy_plaintext", repo.getActiveServerConfig()!!.password)

        // The migration is async (runBlocking in init), so it should have happened
        val persisted = cache.getActiveServerConfig()
        assertTrue(persisted != null && storage.isEncrypted(persisted!!.password))
    }

    @Test
    fun failedDecryptionClearsPasswordToEmpty() = runTest {
        val cache = FakeLocalCache()
        val storage = FailingDecryptSecureTokenStorage()
        cache.insertServerConfig(makeConfig(password = "ENC:some_encrypted_value"))
        cache.setActiveServerConfig("srv_1")

        val repo = ServerConfigRepository(cache, storage)
        // When decryption fails, password should be cleared to empty
        assertEquals("", repo.getActiveServerConfig()!!.password)
    }

    @Test
    fun isLoadedBecomesTrueAfterInit() {
        val cache = FakeLocalCache()
        val repo = ServerConfigRepository(cache, null)
        assertTrue(repo.isLoaded.value)
    }
}

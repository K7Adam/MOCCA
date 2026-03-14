package com.mocca.app.data.local

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Periodically prunes old messages from the database to prevent unbounded growth.
 * Implements the "Lean Cache" strategy by keeping only the most recent N messages
 * per session, reducing database size and query times while preserving the core UI experience.
 */
class DatabasePruner(
    private val localCache: LocalCache
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Configurable limit per session. "Lean Cache" keeps the recent active context.
    private val keepCount: Long = 500L
    
    /**
     * Run the pruning process.
     * Iterates all sessions and removes old messages exceeding the keepCount.
     */
    fun pruneNow() {
        scope.launch {
            try {
                val sessions = localCache.getAllSessions()
                if (sessions.isEmpty()) return@launch
                
                Napier.i("[DatabasePruner] Starting automatic database pruning (keepCount=$keepCount)...")
                
                var prunedSessionsCount = 0
                sessions.forEach { session ->
                    // Prunes messages older than the `keepCount`-th latest message in each session.
                    localCache.pruneMessages(session.id, keepCount)
                    prunedSessionsCount++
                }
                
                Napier.i("[DatabasePruner] Database pruning completed for $prunedSessionsCount sessions.")
            } catch (e: Exception) {
                Napier.e("[DatabasePruner] Error during database pruning", e)
            }
        }
    }
}

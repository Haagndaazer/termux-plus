package com.termux.app.plus.service

import com.termux.terminal.TerminalSession

/**
 * Singleton that tracks which TerminalSessions belong to which connection profiles.
 * Used by ConnectionListActivity to display active terminals per host and by
 * TermuxActivity to register new sessions.
 */
object ActiveSessionTracker {

    private val sessionToProfile = mutableMapOf<TerminalSession, Long>()

    /** Session to resume when TermuxActivity comes to front. */
    @Volatile
    var pendingResumeSession: TerminalSession? = null

    @Synchronized
    fun registerSession(profileId: Long, session: TerminalSession) {
        sessionToProfile[session] = profileId
    }

    @Synchronized
    fun getActiveSessionsForProfile(profileId: Long): List<TerminalSession> {
        cleanup()
        return sessionToProfile.entries
            .filter { it.value == profileId && it.key.isRunning }
            .map { it.key }
    }

    @Synchronized
    fun getActiveSessionCount(profileId: Long): Int {
        return getActiveSessionsForProfile(profileId).size
    }

    @Synchronized
    fun hasActiveSessions(profileId: Long): Boolean {
        return getActiveSessionsForProfile(profileId).isNotEmpty()
    }

    @Synchronized
    fun killSession(session: TerminalSession) {
        session.finishIfRunning()
        sessionToProfile.remove(session)
    }

    @Synchronized
    fun killAllSessionsForProfile(profileId: Long) {
        val sessions = sessionToProfile.entries
            .filter { it.value == profileId }
            .map { it.key }
        sessions.forEach { it.finishIfRunning() }
        sessionToProfile.keys.removeAll(sessions.toSet())
    }

    @Synchronized
    fun killAllSessions() {
        sessionToProfile.keys.forEach { it.finishIfRunning() }
        sessionToProfile.clear()
    }

    @Synchronized
    fun cleanup() {
        sessionToProfile.keys.removeAll { !it.isRunning }
    }
}

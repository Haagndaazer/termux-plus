package com.termux.app.plus.service

import android.content.Context
import android.content.Intent
import com.termux.app.TermuxActivity
import com.termux.app.plus.data.model.ConnectionProfile
import com.termux.app.plus.security.CredentialManager
import java.io.File

/**
 * Builds SSH commands from connection profiles and launches terminal sessions.
 */
object SessionLauncher {

    /**
     * Builds the SSH command string from a ConnectionProfile.
     * For password auth, wraps with sshpass.
     */
    fun buildSshCommand(profile: ConnectionProfile, context: Context): String {
        val sb = StringBuilder()

        // Password auth requires sshpass
        if (profile.authMethod == "PASSWORD" && profile.encryptedPassword != null) {
            val password = CredentialManager.decrypt(profile.encryptedPassword, context)
            val escapedPassword = password.replace("'", "'\\''")
            sb.append("sshpass -p '$escapedPassword' ")
        }

        sb.append("ssh")

        // Keep-alive options
        if (profile.keepAliveEnabled) {
            sb.append(" -o ServerAliveInterval=${profile.keepAliveInterval}")
            sb.append(" -o ServerAliveCountMax=${profile.keepAliveCountMax}")
        }

        // Accept new host keys automatically on first connect
        sb.append(" -o StrictHostKeyChecking=accept-new")

        // Key-based auth
        if (profile.authMethod in listOf("KEY", "KEY_WITH_PASSPHRASE") && profile.privateKeyPath != null) {
            sb.append(" -i ${profile.privateKeyPath}")
        }

        // Port (only add if non-default)
        if (profile.port != 22) {
            sb.append(" -p ${profile.port}")
        }

        // user@host
        sb.append(" ${profile.username}@${profile.host}")

        return sb.toString()
    }

    /**
     * Launches TermuxActivity with the SSH command for the given profile.
     * Always creates a new terminal session.
     */
    fun launchConnection(profile: ConnectionProfile, context: Context) {
        val sshCommand = buildSshCommand(profile, context)
        val intent = Intent(context, TermuxActivity::class.java).apply {
            putExtra("termux_plus_ssh_command", sshCommand)
            putExtra("termux_plus_profile_id", profile.id)
            putExtra("termux_plus_session_name", profile.nickname)
            // FLAG_ACTIVITY_NEW_TASK needed when launching from activity context with singleTask target
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Launches TermuxActivity for a local terminal session (no SSH).
     */
    fun launchLocalTerminal(context: Context) {
        val intent = Intent(context, TermuxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Checks if sshpass is installed in the Termux environment.
     */
    fun isSshpassInstalled(): Boolean {
        val sshpassPath = "/data/data/com.termux/files/usr/bin/sshpass"
        return File(sshpassPath).exists()
    }
}

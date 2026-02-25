package com.termux.app.plus.ui.keys

/**
 * Represents an SSH key pair found in ~/.ssh/
 */
data class SshKeyInfo(
    val name: String,
    val path: String,
    val publicKey: String?,
    val keyType: String?
)

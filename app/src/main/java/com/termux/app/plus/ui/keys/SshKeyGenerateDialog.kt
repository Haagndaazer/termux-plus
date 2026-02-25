package com.termux.app.plus.ui.keys

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.termux.R
import com.termux.shared.termux.TermuxConstants
import java.io.File

class SshKeyGenerateDialog : DialogFragment() {

    var onKeyGenerated: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_ssh_key_generate, null)

        val inputKeyName = view.findViewById<TextInputEditText>(R.id.input_key_name)
        val layoutKeyName = view.findViewById<TextInputLayout>(R.id.layout_key_name)
        val inputKeyType = view.findViewById<AutoCompleteTextView>(R.id.input_key_type)
        val inputPassphrase = view.findViewById<TextInputEditText>(R.id.input_key_passphrase)

        // Setup key type dropdown
        val keyTypes = arrayOf("ed25519", "rsa", "ecdsa")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, keyTypes)
        inputKeyType.setAdapter(typeAdapter)

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.tp_generate_key)
            .setView(view)
            .setPositiveButton(R.string.tp_generate_key) { _, _ ->
                val keyName = inputKeyName.text?.toString()?.trim()
                val keyType = inputKeyType.text?.toString()?.trim() ?: "ed25519"
                val passphrase = inputPassphrase.text?.toString() ?: ""

                if (keyName.isNullOrEmpty()) {
                    layoutKeyName.error = getString(R.string.tp_field_required)
                    return@setPositiveButton
                }

                generateKey(keyName, keyType, passphrase)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun generateKey(name: String, type: String, passphrase: String) {
        val sshDir = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".ssh")
        if (!sshDir.exists()) {
            sshDir.mkdirs()
            // Set proper permissions
            sshDir.setReadable(true, true)
            sshDir.setWritable(true, true)
            sshDir.setExecutable(true, true)
        }

        val keyPath = File(sshDir, name).absolutePath

        // Check if key already exists
        if (File(keyPath).exists()) {
            Toast.makeText(context, "Key '$name' already exists", Toast.LENGTH_SHORT).show()
            return
        }

        // Build ssh-keygen command
        val sshKeygenPath = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/ssh-keygen"
        val args = mutableListOf(sshKeygenPath, "-t", type, "-f", keyPath, "-N", passphrase)

        // Add bits for RSA
        if (type == "rsa") {
            args.addAll(listOf("-b", "4096"))
        }

        try {
            val processBuilder = ProcessBuilder(args)
            processBuilder.environment()["HOME"] = TermuxConstants.TERMUX_HOME_DIR_PATH
            processBuilder.environment()["PATH"] = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Toast.makeText(context, R.string.tp_key_generated, Toast.LENGTH_SHORT).show()
                onKeyGenerated?.invoke()
            } else {
                val output = process.inputStream.bufferedReader().readText()
                Toast.makeText(context, getString(R.string.tp_key_generation_failed) + ": $output", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, getString(R.string.tp_key_generation_failed) + ": ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

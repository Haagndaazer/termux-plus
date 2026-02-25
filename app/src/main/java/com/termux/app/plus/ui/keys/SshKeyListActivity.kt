package com.termux.app.plus.ui.keys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.termux.R
import com.termux.shared.termux.TermuxConstants
import java.io.File

class SshKeyListActivity : AppCompatActivity() {

    private lateinit var adapter: SshKeyAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    private val importKeyLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importKey(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ssh_key_list)

        setupToolbar()
        setupRecyclerView()
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        loadKeys()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.inflateMenu(R.menu.menu_ssh_key_list)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_import_key -> {
                    importKeyLauncher.launch("*/*")
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.keys_recycler_view)
        emptyView = findViewById(R.id.empty_view)

        adapter = SshKeyAdapter(
            onClick = { key -> showPublicKey(key) },
            onLongClick = { key ->
                confirmDeleteKey(key)
                true
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_generate_key).setOnClickListener {
            val dialog = SshKeyGenerateDialog()
            dialog.onKeyGenerated = { loadKeys() }
            dialog.show(supportFragmentManager, "generate_key")
        }
    }

    private fun loadKeys() {
        val sshDir = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".ssh")
        val keys = if (sshDir.exists()) {
            sshDir.listFiles()
                ?.filter { file ->
                    file.isFile &&
                    !file.name.endsWith(".pub") &&
                    file.name != "known_hosts" &&
                    file.name != "config" &&
                    file.name != "authorized_keys"
                }
                ?.map { privateKeyFile ->
                    val pubFile = File(privateKeyFile.path + ".pub")
                    val publicKey = if (pubFile.exists()) pubFile.readText().trim() else null
                    val keyType = publicKey?.split(" ")?.firstOrNull()
                        ?.removePrefix("ssh-")

                    SshKeyInfo(
                        name = privateKeyFile.name,
                        path = privateKeyFile.absolutePath,
                        publicKey = publicKey,
                        keyType = keyType
                    )
                }
                ?.sortedBy { it.name }
                ?: emptyList()
        } else {
            emptyList()
        }

        adapter.submitList(keys)
        emptyView.visibility = if (keys.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (keys.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showPublicKey(key: SshKeyInfo) {
        if (key.publicKey == null) {
            Toast.makeText(this, "No public key found for ${key.name}", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(key.name + ".pub")
            .setMessage(key.publicKey)
            .setPositiveButton(R.string.tp_copied_to_clipboard) { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("SSH Public Key", key.publicKey))
                Toast.makeText(this, R.string.tp_copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteKey(key: SshKeyInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.tp_delete_key)
            .setMessage(getString(R.string.tp_delete_key_confirm, key.name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                deleteKey(key)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteKey(key: SshKeyInfo) {
        File(key.path).delete()
        File(key.path + ".pub").delete()
        loadKeys()
    }

    private fun importKey(uri: Uri) {
        try {
            val sshDir = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".ssh")
            if (!sshDir.exists()) {
                sshDir.mkdirs()
                sshDir.setReadable(true, true)
                sshDir.setWritable(true, true)
                sshDir.setExecutable(true, true)
            }

            val inputStream = contentResolver.openInputStream(uri) ?: return
            val keyContent = inputStream.bufferedReader().readText()
            inputStream.close()

            // Determine filename from URI or use a default
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_key"
            val destFile = File(sshDir, fileName)

            if (destFile.exists()) {
                Toast.makeText(this, "Key '$fileName' already exists", Toast.LENGTH_SHORT).show()
                return
            }

            destFile.writeText(keyContent)
            destFile.setReadable(true, true)
            destFile.setWritable(true, true)

            Toast.makeText(this, R.string.tp_key_imported, Toast.LENGTH_SHORT).show()
            loadKeys()
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

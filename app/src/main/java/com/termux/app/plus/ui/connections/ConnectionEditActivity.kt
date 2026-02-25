package com.termux.app.plus.ui.connections

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.termux.R
import com.termux.shared.termux.TermuxConstants
import kotlinx.coroutines.launch
import java.io.File

class ConnectionEditActivity : AppCompatActivity() {

    private lateinit var viewModel: ConnectionEditViewModel
    private var editingProfileId: Long? = null

    // Views
    private lateinit var inputNickname: TextInputEditText
    private lateinit var inputHost: TextInputEditText
    private lateinit var inputPort: TextInputEditText
    private lateinit var inputUsername: TextInputEditText
    private lateinit var authMethodGroup: ChipGroup
    private lateinit var layoutPassword: TextInputLayout
    private lateinit var inputPassword: TextInputEditText
    private lateinit var layoutKeyPath: TextInputLayout
    private lateinit var inputKeyPath: AutoCompleteTextView
    private lateinit var layoutPassphrase: TextInputLayout
    private lateinit var inputPassphrase: TextInputEditText
    private lateinit var switchKeepAlive: MaterialSwitch
    private lateinit var keepAliveOptions: View
    private lateinit var inputKeepAliveInterval: TextInputEditText
    private lateinit var inputKeepAliveCount: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_edit)

        viewModel = ViewModelProvider(this)[ConnectionEditViewModel::class.java]
        editingProfileId = intent.getLongExtra("profile_id", -1).takeIf { it != -1L }

        setupToolbar()
        bindViews()
        setupAuthMethodToggle()
        setupKeepAliveToggle()
        setupKeyPathDropdown()
        setupSaveButton()

        if (editingProfileId != null) {
            loadExistingProfile()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = if (editingProfileId != null) {
            getString(R.string.tp_edit_connection)
        } else {
            getString(R.string.tp_new_connection)
        }
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun bindViews() {
        inputNickname = findViewById(R.id.input_nickname)
        inputHost = findViewById(R.id.input_host)
        inputPort = findViewById(R.id.input_port)
        inputUsername = findViewById(R.id.input_username)
        authMethodGroup = findViewById(R.id.auth_method_group)
        layoutPassword = findViewById(R.id.layout_password)
        inputPassword = findViewById(R.id.input_password)
        layoutKeyPath = findViewById(R.id.layout_key_path)
        inputKeyPath = findViewById(R.id.input_key_path)
        layoutPassphrase = findViewById(R.id.layout_passphrase)
        inputPassphrase = findViewById(R.id.input_passphrase)
        switchKeepAlive = findViewById(R.id.switch_keep_alive)
        keepAliveOptions = findViewById(R.id.keep_alive_options)
        inputKeepAliveInterval = findViewById(R.id.input_keep_alive_interval)
        inputKeepAliveCount = findViewById(R.id.input_keep_alive_count)
    }

    private fun setupAuthMethodToggle() {
        authMethodGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            when (checkedId) {
                R.id.chip_auth_password -> {
                    layoutPassword.visibility = View.VISIBLE
                    layoutKeyPath.visibility = View.GONE
                    layoutPassphrase.visibility = View.GONE
                }
                R.id.chip_auth_key -> {
                    layoutPassword.visibility = View.GONE
                    layoutKeyPath.visibility = View.VISIBLE
                    layoutPassphrase.visibility = View.GONE
                }
                R.id.chip_auth_key_passphrase -> {
                    layoutPassword.visibility = View.GONE
                    layoutKeyPath.visibility = View.VISIBLE
                    layoutPassphrase.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupKeepAliveToggle() {
        switchKeepAlive.setOnCheckedChangeListener { _, isChecked ->
            keepAliveOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupKeyPathDropdown() {
        val sshDir = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".ssh")
        val keyFiles = if (sshDir.exists()) {
            sshDir.listFiles()
                ?.filter { it.isFile && !it.name.endsWith(".pub") && it.name != "known_hosts" && it.name != "config" && it.name != "authorized_keys" }
                ?.map { it.absolutePath }
                ?: emptyList()
        } else {
            emptyList()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, keyFiles)
        inputKeyPath.setAdapter(adapter)

        if (keyFiles.isEmpty()) {
            layoutKeyPath.helperText = getString(R.string.tp_no_keys_found)
        }
    }

    private fun setupSaveButton() {
        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            if (validate()) {
                save()
            }
        }
    }

    private fun loadExistingProfile() {
        lifecycleScope.launch {
            val profile = viewModel.getProfile(editingProfileId!!) ?: return@launch

            inputNickname.setText(profile.nickname)
            inputHost.setText(profile.host)
            inputPort.setText(profile.port.toString())
            inputUsername.setText(profile.username)

            when (profile.authMethod) {
                "PASSWORD" -> authMethodGroup.check(R.id.chip_auth_password)
                "KEY" -> authMethodGroup.check(R.id.chip_auth_key)
                "KEY_WITH_PASSPHRASE" -> authMethodGroup.check(R.id.chip_auth_key_passphrase)
            }

            profile.privateKeyPath?.let { inputKeyPath.setText(it) }

            switchKeepAlive.isChecked = profile.keepAliveEnabled
            inputKeepAliveInterval.setText(profile.keepAliveInterval.toString())
            inputKeepAliveCount.setText(profile.keepAliveCountMax.toString())
        }
    }

    private fun validate(): Boolean {
        var valid = true

        if (inputNickname.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_nickname).error = getString(R.string.tp_field_required)
            valid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_nickname).error = null
        }

        if (inputHost.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_host).error = getString(R.string.tp_field_required)
            valid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_host).error = null
        }

        if (inputUsername.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_username).error = getString(R.string.tp_field_required)
            valid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_username).error = null
        }

        val portText = inputPort.text?.toString()
        val port = portText?.toIntOrNull()
        if (port == null || port < 1 || port > 65535) {
            findViewById<TextInputLayout>(R.id.layout_port).error = getString(R.string.tp_invalid_port)
            valid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_port).error = null
        }

        return valid
    }

    private fun getSelectedAuthMethod(): String {
        return when (authMethodGroup.checkedChipId) {
            R.id.chip_auth_password -> "PASSWORD"
            R.id.chip_auth_key -> "KEY"
            R.id.chip_auth_key_passphrase -> "KEY_WITH_PASSPHRASE"
            else -> "KEY"
        }
    }

    private fun save() {
        val authMethod = getSelectedAuthMethod()
        val password = when (authMethod) {
            "PASSWORD" -> inputPassword.text?.toString()
            "KEY_WITH_PASSPHRASE" -> inputPassphrase.text?.toString()
            else -> null
        }

        viewModel.saveProfile(
            existingId = editingProfileId,
            nickname = inputNickname.text.toString().trim(),
            host = inputHost.text.toString().trim(),
            port = inputPort.text.toString().toIntOrNull() ?: 22,
            username = inputUsername.text.toString().trim(),
            authMethod = authMethod,
            password = password,
            privateKeyPath = if (authMethod != "PASSWORD") inputKeyPath.text?.toString()?.trim() else null,
            keepAliveEnabled = switchKeepAlive.isChecked,
            keepAliveInterval = inputKeepAliveInterval.text.toString().toIntOrNull() ?: 60,
            keepAliveCountMax = inputKeepAliveCount.text.toString().toIntOrNull() ?: 3,
            onComplete = {
                runOnUiThread {
                    Toast.makeText(this, R.string.tp_connection_saved, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        )
    }
}

package com.termux.app.plus.ui.snippets

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.termux.R
import com.termux.app.plus.data.model.Snippet

class SnippetEditDialog : DialogFragment() {

    var existingSnippet: Snippet? = null
    var onSave: ((name: String, command: String, autoExecute: Boolean) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val nameLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.tp_snippet_name)
        }
        val nameInput = TextInputEditText(requireContext()).apply {
            setText(existingSnippet?.name ?: "")
        }
        nameLayout.addView(nameInput)
        container.addView(nameLayout)

        val commandLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.tp_snippet_command)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 16
            layoutParams = params
        }
        val commandInput = TextInputEditText(requireContext()).apply {
            setText(existingSnippet?.command ?: "")
            minLines = 2
            maxLines = 5
            setTypeface(android.graphics.Typeface.MONOSPACE)
        }
        commandLayout.addView(commandInput)
        container.addView(commandLayout)

        val autoExecuteSwitch = MaterialSwitch(requireContext()).apply {
            text = getString(R.string.tp_auto_execute)
            isChecked = existingSnippet?.autoExecute ?: true
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 32
            layoutParams = params
        }
        container.addView(autoExecuteSwitch)

        val title = if (existingSnippet != null) R.string.tp_edit_snippet else R.string.tp_add_snippet

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton(R.string.tp_save) { _, _ ->
                val name = nameInput.text?.toString()?.trim() ?: return@setPositiveButton
                val command = commandInput.text?.toString() ?: return@setPositiveButton
                if (name.isNotEmpty() && command.isNotEmpty()) {
                    onSave?.invoke(name, command, autoExecuteSwitch.isChecked)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}

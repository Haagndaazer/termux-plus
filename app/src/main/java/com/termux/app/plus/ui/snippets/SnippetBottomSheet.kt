package com.termux.app.plus.ui.snippets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.app.plus.data.db.TermuxPlusDatabase
import com.termux.app.plus.data.model.Snippet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class SnippetBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_snippet_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.snippets_recycler_view)
        val emptyView = view.findViewById<TextView>(R.id.empty_view)

        val adapter = SnippetAdapter(
            onClick = { snippet -> executeSnippet(snippet) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val dao = TermuxPlusDatabase.getInstance(requireContext()).snippetDao()
        viewLifecycleOwner.lifecycleScope.launch {
            dao.getAll().collectLatest { snippets ->
                adapter.submitList(snippets)
                emptyView.visibility = if (snippets.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (snippets.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun executeSnippet(snippet: Snippet) {
        val activity = requireActivity() as? TermuxActivity ?: return
        val session = activity.currentSession
        if (session != null) {
            // Write the command text
            val cmdBytes = snippet.command.toByteArray(StandardCharsets.UTF_8)
            session.write(cmdBytes, 0, cmdBytes.size)
            // Send Enter key (carriage return) to execute, if auto-execute is enabled
            if (snippet.autoExecute) {
                session.write(byteArrayOf(13), 0, 1) // '\r' = 13
            }
        }
        dismiss()
    }
}

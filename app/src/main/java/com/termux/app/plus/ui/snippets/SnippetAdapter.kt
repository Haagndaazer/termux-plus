package com.termux.app.plus.ui.snippets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.plus.data.model.Snippet

class SnippetAdapter(
    private val onClick: (Snippet) -> Unit,
    private val onLongClick: ((Snippet) -> Boolean)? = null
) : ListAdapter<Snippet, SnippetAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_snippet, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.snippet_name)
        private val command: TextView = itemView.findViewById(R.id.snippet_command)

        fun bind(snippet: Snippet) {
            name.text = snippet.name
            command.text = snippet.command
            itemView.setOnClickListener { onClick(snippet) }
            onLongClick?.let { handler ->
                itemView.setOnLongClickListener { handler(snippet) }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Snippet>() {
            override fun areItemsTheSame(old: Snippet, new: Snippet) = old.id == new.id
            override fun areContentsTheSame(old: Snippet, new: Snippet) = old == new
        }
    }
}

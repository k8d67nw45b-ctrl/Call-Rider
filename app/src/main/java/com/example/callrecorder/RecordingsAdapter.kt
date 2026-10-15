package com.example.callrecorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingItem(val file: File)

class RecordingsAdapter(
    private val items: MutableList<RecordingItem>,
    private val onPlay: (RecordingItem) -> Unit,
    private val onDelete: (RecordingItem, Int) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvFileMeta: TextView = view.findViewById(R.id.tvFileMeta)
        val btnPlay: ImageButton = view.findViewById(R.id.btnPlay)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvFileName.text = item.file.name

        val sizeKb = item.file.length() / 1024
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            .format(Date(item.file.lastModified()))
        holder.tvFileMeta.text = "$dateStr  •  ${sizeKb} KB"

        holder.btnPlay.setOnClickListener { onPlay(item) }
        holder.btnDelete.setOnClickListener {
            onDelete(item, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int = items.size

    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateItems(newItems: List<RecordingItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}

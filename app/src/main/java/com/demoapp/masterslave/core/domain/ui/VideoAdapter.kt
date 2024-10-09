package com.demoapp.masterslave.core.domain.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.demoapp.masterslave.R
import com.demoapp.masterslave.core.domain.model.VideoFile

class VideoAdapter : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private val selectedVideos = mutableSetOf<VideoFile>()

    private val diffUtil = object : DiffUtil.ItemCallback<VideoFile>() {
        override fun areItemsTheSame(oldItem: VideoFile, newItem: VideoFile): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: VideoFile, newItem: VideoFile): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.video_item_layout, parent, false)
        return VideoViewHolder(
            view
        )
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = differ.currentList[position]
        holder.videoName.text = video.name
        holder.checkBox.isChecked = selectedVideos.contains(video)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedVideos.add(video)
            } else {
                selectedVideos.remove(video)
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun getSelectedVideos(): List<VideoFile> {
        return selectedVideos.toList()
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoName: TextView = itemView.findViewById(R.id.videoNameTextView)
        val checkBox: CheckBox = itemView.findViewById(R.id.videoCheckbox)
    }
}

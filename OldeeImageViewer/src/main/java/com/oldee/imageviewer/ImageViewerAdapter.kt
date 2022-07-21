package com.oldee.imageviewer

import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.oldee.imageviewer.databinding.LayoutImageViewerItemBinding

class ImageViewerAdapter(
    private val dataSets: List<String>,
    private val imageCallBack: (ImageView, String) -> Unit
) : RecyclerView.Adapter<ImageViewerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ImageViewerViewHolder.from(parent)

    override fun onBindViewHolder(holder: ImageViewerViewHolder, position: Int) {
        holder.bind(dataSets[position], imageCallBack)
    }

    override fun getItemCount() = dataSets.size

    fun init(){
        notifyDataSetChanged()
    }
}

class ImageViewerViewHolder(private val binding: LayoutImageViewerItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

//    private var mScaleGestureDetector: ScaleGestureDetector? = null

    companion object {
        fun from(parent: ViewGroup): ImageViewerViewHolder {
            val v = LayoutImageViewerItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            return ImageViewerViewHolder(v)
        }
    }

    fun bind(path: String, imageCallBack: (ImageView, String) -> Unit) {
        imageCallBack.invoke(binding.ivImage, path)
    }
}
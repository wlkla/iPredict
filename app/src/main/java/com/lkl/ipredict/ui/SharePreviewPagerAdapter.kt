package com.lkl.ipredict.ui

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.lkl.ipredict.R

class SharePreviewPagerAdapter(
    private val bitmaps: List<Bitmap>
) : RecyclerView.Adapter<SharePreviewPagerAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_share_preview, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(bitmaps[position])
    }

    override fun getItemCount(): Int = bitmaps.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val img = view.findViewById<ImageView>(R.id.imgPreview)
        fun bind(bitmap: Bitmap) {
            img.setImageBitmap(bitmap)
        }
    }
}

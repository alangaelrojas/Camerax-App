package com.apps.aggr.cameraxapp.app.listphotos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.apps.aggr.cameraxapp.R
import com.bumptech.glide.Glide
import java.io.File

class ListPhotosTakenAdapter(
    private val context: Context,
    private val onPhotoSelected: OnPhotoSelected
) : RecyclerView.Adapter<ListPhotosTakenAdapter.holderPhotos>() {

    private val listPhotos = ArrayList<File>()

    fun addPhotos(array: Array<File>){
        listPhotos.clear()
        listPhotos.addAll(array)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): holderPhotos {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_photo, parent, false )
        return holderPhotos(view)
    }

    override fun onBindViewHolder(holder: holderPhotos, position: Int) {
        Glide.with(context)
            .load(listPhotos[position])
            .into(holder.photo)
    }

    override fun getItemCount(): Int = listPhotos.size

    inner class holderPhotos(view: View) : RecyclerView.ViewHolder(view){
        val photo = view.findViewById<ImageView>(R.id.img_photo)

        init {
            photo.setOnClickListener {
                val photo = listPhotos[adapterPosition]
                onPhotoSelected.onClickPhoto(photo.absolutePath)
            }
        }
    }

    interface OnPhotoSelected{
        fun onClickPhoto(path: String)
    }
}
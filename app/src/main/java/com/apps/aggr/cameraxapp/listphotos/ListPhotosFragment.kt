package com.apps.aggr.cameraxapp.listphotos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.apps.aggr.cameraxapp.R
import com.apps.aggr.cameraxapp.cropphoto.ImageCropperActivity
import kotlinx.android.synthetic.main.fragment_list_photos.*
import java.io.File

class ListPhotosFragment : Fragment(), ListPhotosAdapter.OnPhotoSelected {

    private lateinit var listAdapter: ListPhotosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        listAdapter = ListPhotosAdapter(requireContext(), this)
        return inflater.inflate(R.layout.fragment_list_photos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.apply {
            navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_back)
            setNavigationOnClickListener{
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        rvPhotos.apply {
            layoutManager = GridLayoutManager(context, 3)
            setHasFixedSize(true)
            adapter = listAdapter
        }
        listAdapter.addPhotos(showListFiles())
    }

    private fun showListFiles(): Array<File> {
        val folder = activity?.externalMediaDirs?.first().toString()
        val directory = File(folder)
        return directory.listFiles()?: emptyArray()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ListPhotosFragment()
    }

    override fun onClickPhoto(path: String) {
        val intent = Intent(requireContext(), ImageCropperActivity::class.java)
        intent.putExtra(ImageCropperActivity.IMAGE_EXTRA_NAME, path)
        startActivity(intent)
    }
}
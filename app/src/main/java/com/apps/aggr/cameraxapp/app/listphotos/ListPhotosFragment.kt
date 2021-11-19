package com.apps.aggr.cameraxapp.app.listphotos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.apps.aggr.cameraxapp.R
import com.apps.aggr.cameraxapp.app.cropphoto.ImageCropperActivity
import com.apps.aggr.cameraxapp.utils.Constants.CROPPED_PATH_FOLDER
import com.apps.aggr.cameraxapp.utils.Constants.NORMAL_PATH_FOLDER
import kotlinx.android.synthetic.main.fragment_list_photos.*
import java.io.File

class ListPhotosFragment : Fragment(), ListPhotosTakenAdapter.OnPhotoSelected,
    ListPhotosCroppedAdapter.OnPhotoCroppedSelected {

    private lateinit var listTakenAdapter: ListPhotosTakenAdapter
    private lateinit var listCroppedAdapter: ListPhotosCroppedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        listTakenAdapter = ListPhotosTakenAdapter(requireContext(), this)
        listCroppedAdapter = ListPhotosCroppedAdapter(requireContext(), this)
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
            adapter = listTakenAdapter
        }
        rvPhotosCropped.apply {
            layoutManager = GridLayoutManager(context, 3)
            setHasFixedSize(true)
            adapter = listCroppedAdapter
        }
    }

    override fun onStart() {
        super.onStart()
        listTakenAdapter.addPhotos(showListFiles())
        listCroppedAdapter.addPhotos(showListFilesCropped())
    }

    private fun showListFiles(): Array<File> {
        val folder = "${activity?.externalCacheDirs?.first()}$NORMAL_PATH_FOLDER"
        val directory = File(folder)
        return directory.listFiles()?: emptyArray()
    }

    private fun showListFilesCropped(): Array<File> {
        val folder = "${activity?.externalCacheDirs?.first()}$CROPPED_PATH_FOLDER"
        val directory = File(folder)
        return directory.listFiles()?: emptyArray()
    }

    override fun onClickPhoto(path: String) {
        ImageCropperActivity.launchCropper(requireContext(), path)
    }

    override fun onClickPhotoCropped(path: String) {

    }
}
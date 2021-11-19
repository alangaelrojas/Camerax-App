package com.apps.aggr.cameraxapp

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.apps.aggr.cameraxapp.takephoto.CameraXFragment

class MainActivity : AppCompatActivity(), LifecycleOwner {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //make the activity fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN)

        //Set view
        setContentView(R.layout.activity_main)


        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment, CameraXFragment.newInstance())
            .commit()

    }

    override fun onBackPressed() {
        val fragment = this.supportFragmentManager.backStackEntryCount
        if (fragment == 0) super.onBackPressed()
        else supportFragmentManager.popBackStack()
    }

    companion object {

        fun changeFragment(activity: FragmentActivity, fragment: Fragment){
           activity.supportFragmentManager
               .beginTransaction()
               .replace(R.id.fragment, fragment)
               .addToBackStack(fragment.tag)
               .commit()
        }

    }
}

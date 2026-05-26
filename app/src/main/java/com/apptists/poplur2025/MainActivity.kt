package com.apptists.poplur2025

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.apptists.poplur2025.ui.PoplurRoot

import androidx.fragment.app.FragmentActivity
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

class MainActivity : FragmentActivity(), AndroidFragmentApplication.Callbacks {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.ACCESS_FINE_LOCATION))
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { PoplurRoot() } } }
    }

    override fun exit() {
        finish()
    }
}

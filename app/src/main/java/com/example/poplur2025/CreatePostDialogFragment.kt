package com.example.poplur2025

// CreatePostDialogFragment.kt

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

// Assume MediaType enum is defined and accessible
// enum class MediaType { NONE, IMAGE, VIDEO }

class CreatePostDialogFragment : DialogFragment() {

    interface PostCreationListener {
        fun onPostCreated(
            latLng: LatLng,
            textContent: String?,
            mediaType: MediaType,
            mediaUrl: String?,
            radius: Double
        )
    }

    private var listener: PostCreationListener? = null
    private lateinit var clickedLatLng: LatLng

    private lateinit var editTextContent: EditText
    private lateinit var imageViewSelectedMedia: ImageView
    private lateinit var buttonSelectImage: Button

    // TODO: Add buttonSelectVideo
    private lateinit var seekBarRadius: SeekBar
    private lateinit var textViewRadiusValue: TextView
    private lateinit var buttonSubmitPost: Button

    private var selectedMediaUri: Uri? = null
    private var currentMediaType: MediaType = MediaType.NONE

    // ActivityResultLauncher for picking images
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                result.data?.data?.let { uri ->
                    selectedMediaUri = uri
                    imageViewSelectedMedia.setImageURI(uri)
                    imageViewSelectedMedia.visibility = View.VISIBLE
                    currentMediaType = MediaType.IMAGE
                }
            }
        }
    // TODO: Add a pickVideoLauncher similar to pickImageLauncher using Intent.ACTION_PICK and MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            clickedLatLng = it.getParcelable(ARG_LAT_LNG)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // If using a custom layout for the DialogFragment's content view:
        val view = inflater.inflate(R.layout.dialog_create_post, container, false)
        // Initialize your views here (editTextContent, imageViewSelectedMedia, etc.)
        // This is preferred over onCreateDialog for custom layouts.
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views from R.layout.dialog_create_post
        editTextContent = view.findViewById(R.id.et_post_text_content)
        imageViewSelectedMedia = view.findViewById(R.id.iv_selected_media_preview)
        buttonSelectImage = view.findViewById(R.id.btn_select_image)
        seekBarRadius = view.findViewById(R.id.seekbar_radius)
        textViewRadiusValue = view.findViewById(R.id.tv_radius_value)
        buttonSubmitPost = view.findViewById(R.id.btn_submit_post)

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }
        // TODO: Set up buttonSelectVideo OnClickListener

        seekBarRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textViewRadiusValue.text = "$progress m"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Set initial radius (e.g., 50m default, max 500m)
        seekBarRadius.max = 500
        seekBarRadius.progress = 50
        textViewRadiusValue.text = "${seekBarRadius.progress} m"


        buttonSubmitPost.setOnClickListener {
            val textContent = editTextContent.text.toString().trim()
            val radius = seekBarRadius.progress.toDouble()

            if (selectedMediaUri != null) {
                uploadMediaAndCreatePost(textContent, radius)
            } else {
                // No media selected, create post directly
                listener?.onPostCreated(
                    clickedLatLng,
                    textContent.ifEmpty { null },
                    MediaType.NONE,
                    null,
                    radius
                )
                dismiss()
            }
        }
    }

    private fun uploadMediaAndCreatePost(textContent: String?, radius: Double) {
        val uri = selectedMediaUri ?: return
        val storageRef = FirebaseStorage.getInstance().reference
        // Create a unique filename (e.g., using UUID)
        val filename = "posts_media/${UUID.randomUUID()}"
        val mediaRef = storageRef.child(filename)

        // Show a progress indicator here if needed
        Toast.makeText(context, "Uploading media...", Toast.LENGTH_SHORT).show()

        mediaRef.putFile(uri)
            .addOnSuccessListener {
                mediaRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Toast.makeText(context, "Upload successful!", Toast.LENGTH_SHORT).show()
                    listener?.onPostCreated(
                        clickedLatLng,
                        textContent?.ifEmpty { null },
                        currentMediaType, // Use the detected media type
                        downloadUrl.toString(),
                        radius
                    )
                    dismiss()
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to get download URL.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            .addOnProgressListener { snapshot ->
                // Optionally update progress
                val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount)
                Log.d(TAG, "Upload is $progress% done")
            }
    }


    // Optional: If you prefer using AlertDialog.Builder for simpler dialogs (no custom layout needed from onCreateView)
    /*
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_create_post, null) // Your custom layout

        // Initialize views (editTextContent, etc.) from 'view'
        editTextContent = view.findViewById(R.id.et_post_text_content)
        // ... (initialize other views)

        builder.setView(view)
            .setTitle("Create New Post")
            .setPositiveButton("Post") { dialog, id ->
                val text = editTextContent.text.toString()
                val radius = 50.0 // Get from SeekBar in your layout

                // TODO: Handle media selection/upload and get mediaUrl and mediaType
                // For now, assuming no media:
                if (selectedMediaUri != null) {
                   // Start media upload, then call listener in the success callback
                   uploadMediaAndCreatePost(text, radius)
                } else {
                   listener?.onPostCreated(clickedLatLng, text, MediaType.NONE, null, radius)
                }

            }
            .setNegativeButton("Cancel") { dialog, id ->
                getDialog()?.cancel()
            }
        return builder.create()
    }
    */


    fun setPostCreationListener(listener: MapFragment) {
        this.listener = listener
    }

    companion object {
        const val TAG = "CreatePostDialog"
        private const val ARG_LAT_LNG = "arg_lat_lng"

        @JvmStatic
        fun newInstance(latLng: LatLng) =
            CreatePostDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_LAT_LNG, latLng)
                }
            }
    }
}
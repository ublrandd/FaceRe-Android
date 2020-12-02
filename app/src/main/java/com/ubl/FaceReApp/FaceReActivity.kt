package com.ubl.FaceReApp

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ubl.FaceRe.BoundingBoxOverlay
import com.ubl.FaceRe.FaceRe
import java.net.URL
import java.util.concurrent.Executors


class FaceReActivity : AppCompatActivity() {

    var faceRe = FaceRe()
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS =
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private lateinit var cameraTextureView: TextureView

    private var progressDialog: ProgressDialog? = null


//    // For testing purposes only!
//    companion object {
//        // This view's VISIBILITY is set to View.GONE in activity_main.xml
//        lateinit var logTextView: TextView
//        fun setMessage(message: String) {
//            logTextView.text = message
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_re)

        findViewById<TextView>(R.id.StudentName).text = "Student Name"

        findViewById<TextView>(R.id.StudentID).text = "Student ID"

        faceRe.IntializeModel(this, rearCamera = false)

        // Implementation of CameraX preview
        cameraTextureView = findViewById(R.id.camera_textureView)
        val boundingBoxOverlay = findViewById<BoundingBoxOverlay>(R.id.bbox_overlay)
//        logTextView = findViewById(R.id.logTextView)

        if (allPermissionsGranted()) {
            cameraTextureView.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        cameraTextureView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            faceRe.updateTransform(cameraTextureView)
        }

        // Necessary to keep the Overlay above the TextureView so that the boxes are visible.
        boundingBoxOverlay.setWillNotDraw(false)
        boundingBoxOverlay.setZOrderOnTop(true)
//        frameAnalyser = FrameAnalyser(this, boundingBoxOverlay)

        val resources = mapOf(
            "accuracy" to findViewById(R.id.latestaccuracy) as TextView,
            "retry" to findViewById(R.id.retry) as Button,
            "skip" to findViewById(R.id.skip) as Button
        )

        resources["retry"]?.setOnClickListener {
            finish()
            startActivity(intent)
        }

        resources["skip"]?.setOnClickListener {
            navigateToNewActivity()
        }


        faceRe.InitializeFrame(boundingBoxOverlay, this, ::successCallbackFunction, resources)


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            // Read image data
            LoadImageToCompare()
        }
    }

//    private fun onAlertDialog(view: Context) {
//        //Instantiate builder variable
//        val builder = AlertDialog.Builder(view)
//
//        // set title
//        builder.setTitle("Alert Message")
//
//        //set content area
//        builder.setMessage("Prediction accuracy is too low i.e. ${faceRe.frameAnalyser.finalAverage}")
//
//        //set negative button
//        builder.setPositiveButton(
//            "Retry"
//        ) { dialog, id ->
//            val intent = intent
//            finish()
//            startActivity(intent)
//        }
//
//        //set positive button
//        builder.setNegativeButton(
//            "Cancel"
//        ) { dialog, id ->
//            // User cancelled the dialog
//        }
////        //set neutral button
////        builder.setNeutralButton("Reminder me latter") { dialog, id ->
////            // User Click on reminder me latter
////        }
//        builder.show()
//    }

    private fun successCallbackFunction() {
        if (faceRe.frameAnalyser.finalAverage < faceRe.frameAnalyser.maxScore) {
//            onAlertDialog(this)
            CameraX.unbindAll()
        } else {
            val toastMessage = Toast.makeText(this, "Success", Toast.LENGTH_LONG)
            toastMessage.show()
            navigateToNewActivity()
        }
        return
    }

    private fun navigateToNewActivity(){
        val i = Intent(this, NewActivity::class.java)
        finish() //Kill the activity from which you will go to next activity
        startActivity(i)
    }

    private fun LoadImageToCompare() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {

            faceRe.LoadImageUrlToCompare(
                URL("https://4.bp.blogspot.com/-HBz-6BgylPc/WJArnxlNSZI/AAAAAAAAAZw/IHM5Ug2KmLcCmyKd9BsGo7f-p0kIc_M5gCLcB/s1600/Hd%2BBlur%2BEditor2016_11_06_22_17_35.jpg"),
                "Sushant"
            )

            //load bitmap example
//            faceRe.LoadBitmapToCompare(bmp: Bitmap, FaceName: "Sushant")

        }
    }


    // Start the camera preview once the permissions are granted.
    private fun startCamera() {

        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.FRONT)
        }.build()
        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            val parent = cameraTextureView.parent as ViewGroup
            parent.removeView(cameraTextureView)
            parent.addView(cameraTextureView, 0)
            cameraTextureView.surfaceTexture = it.surfaceTexture
            faceRe.updateTransform(cameraTextureView)
        }

        // FrameAnalyser -> fetches camera frames and makes them in the analyse() method.
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE
            )
            setLensFacing(CameraX.LensFacing.FRONT)
        }.build()

        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(Executors.newSingleThreadExecutor(), faceRe.frameAnalyser)
        }

        // Bind the preview and frameAnalyser.
        CameraX.bindToLifecycle(this, preview, analyzerUseCase)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraTextureView.post { startCamera() }
                LoadImageToCompare()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


}

package me.jameshunt.remotecamera

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import io.minio.MinioClient
import io.minio.UploadObjectArgs
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageCapture = ImageCapture.Builder()
            .setFlashMode(FLASH_MODE_ON)
            .build()

        val saveFile = File(this.cacheDir, "image.jpg")
        val mainExecutor = ContextCompat.getMainExecutor(this)
        val cameraProvider = ProcessCameraProvider.getInstance(this)
        cameraProvider.addListener({
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.get().bindToLifecycle(this, cameraSelector, imageCapture)

            val outputFileOptions = ImageCapture.OutputFileOptions
                .Builder(saveFile)
                .build()

            Handler(Looper.getMainLooper()).postDelayed({
                imageCapture.takePicture(outputFileOptions, mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(error: ImageCaptureException) {
                            // TODO: mqtt publish error
                            finish()
                        }

                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.d(
                                "Main", "took picture with size: ${saveFile.readBytes().size}"
                            )
                            uploadTimeStampedFile(saveFile)
                            // TODO: mqtt publish url
                        }
                    }
                )
            }, 1000)
        }, mainExecutor)
    }

    fun uploadTimeStampedFile(file: File) {
        Executors.newSingleThreadExecutor().execute {
            val time = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            getMinioClient().uploadObject(
                UploadObjectArgs.builder()
                    .bucket("images")
                    .`object`("$time.jpg")
                    .filename(file.absolutePath)
                    .build()
            )
            this@MainActivity.finish()
        }
    }

    private fun getMinioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(Secrets.endpoint)
            .credentials(Secrets.id, Secrets.key)
            .build()
    }
}
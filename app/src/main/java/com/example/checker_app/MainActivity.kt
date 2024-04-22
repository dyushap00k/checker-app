package com.example.checker_app

import  androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var gridOverlayView: GridOverlayView
    private lateinit var imageCapture: ImageCapture // Объявляем переменную imageCapture в классе MainActivity

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CAMERA_PERMISSION = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        gridOverlayView = findViewById(R.id.gridOverlayView)
        val captureButton: Button = findViewById(R.id.captureButton)
        captureButton.setOnClickListener {
            takePhoto()
        }
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build() // Создаем ImageCapture здесь

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture // Добавляем imageCapture в bindToLifecycle
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Ошибка при использовании камеры", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Разрешение на использование камеры отклонено",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    private fun takePhoto() {
        if (!::imageCapture.isInitialized) {
            Log.e(TAG, "ImageCapture не инициализирован")
            return
        }

        // Получение временной директории для сохранения изображения
        val photoFile = File(externalMediaDirs.firstOrNull(), "${System.currentTimeMillis()}.jpg")

        // Создание запроса для захвата изображения
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Захват изображения
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Фото успешно сохранено, можно обновить UI или выполнить другие действия
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Фото сохранено: ${photoFile.absolutePath}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    // Произошла ошибка при сохранении фото
                    Log.e(TAG, "Ошибка при сохранении фото", exception)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Ошибка при сохранении фото", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }


}

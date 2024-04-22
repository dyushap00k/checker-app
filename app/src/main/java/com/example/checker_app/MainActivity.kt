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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Socket
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
        private const val SERVER_IP = "192.168.0.100"
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

        // Захват изображения
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)

                    // Выполнение сетевой операции в фоновом потоке
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Отправка байтов на сервер
                            val socket = Socket(SERVER_IP, 12345)
                            val outputStream = socket.getOutputStream()
                            outputStream.write(bytes)
                            outputStream.close()
                            socket.close()

                            // Обновление UI (показать сообщение об успешной отправке)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Фото успешно отправлено на сервер", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            // Обработка ошибок
                            Log.e(TAG, "Ошибка при отправке фото на сервер", e)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Ошибка при отправке фото на сервер", Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            image.close()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    // Произошла ошибка при захвате фото
                    Log.e(TAG, "Ошибка при захвате фото", exception)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Ошибка при захвате фото", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }





}

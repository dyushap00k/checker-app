package com.example.checker_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private PreviewView viewFinder;
    private ImageCapture imageCapture;

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 123;
    private static final String SERVER_IP = "192.168.0.100";
    private ApiService retrofitInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFinder = findViewById(R.id.viewFinder);
        Button captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(view -> takePhoto());
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        }
    }

    private void startCamera() {
        try {
            ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

            imageCapture = new ImageCapture.Builder().build();

            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
            );
        } catch (Exception exc) {
            Log.e(TAG, "Error occurred while using camera", exc);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(
                        this,
                        "Permission to use camera denied",
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            }
        }
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture not initialized");
            return;
        }

        imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        Retrofit retrofit = new Retrofit.Builder()
                                .baseUrl("http://192.168.0.100:8080/")
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();

                        retrofitInterface = retrofit.create(ApiService.class);

                        // Отправка фотографии на сервер
//                        sendPhotoToServer(bytes);
                        sendDataToServer(bytes);
                        image.close();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Error capturing photo", exception);
                        Toast.makeText(
                                MainActivity.this,
                                "Error capturing photo",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }


    private void sendDataToServer(byte[] bytes) {
        Map<String, String> json = new HashMap<>();
        String imageData = Base64.encodeToString(bytes, Base64.DEFAULT);
        json.put("image_data", imageData);
        Call<ResponseBody> call = retrofitInterface.sendData(json);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Прочитать данные из ResponseBody
                    try {
                        String responseData = response.body().string();
                        System.out.println(responseData);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("MainActivity", "Failed to read response data");
                    }
                } else {
                    Log.e("MainActivity", "Failed to send data");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("MainActivity", "Error: " + t.getMessage());
            }
        });
    }
}

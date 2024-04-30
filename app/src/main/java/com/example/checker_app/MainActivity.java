package com.example.checker_app;

import static com.example.checker_app.FullScreenImageActivity.EXTRA_IMAGE_BYTES;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

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
    private ApiService retrofitInterface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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

            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            Preview preview = new Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build();
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

            imageCapture = new ImageCapture.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.0.100:8080/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            retrofitInterface = retrofit.create(ApiService.class);

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
                        processCapturedImage(image);
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

    private void processCapturedImage(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        image.close();

        sendDataToServer(bytes);
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
                    try {
                        assert response.body() != null;
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        String imageData = jsonObject.getString("compressed_data");
                        byte[] imageBytes = Base64.decode(imageData, Base64.DEFAULT);

                        // Создаем новый интент для запуска новой активности и передаем в него изображение
                        Intent intent = new Intent(MainActivity.this, FullScreenImageActivity.class);
                        intent.putExtra(EXTRA_IMAGE_BYTES, imageBytes);
                        startActivity(intent);

                    } catch (IOException | JSONException e) {
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

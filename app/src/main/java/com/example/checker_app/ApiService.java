package com.example.checker_app;


import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/")
    Call<ResponseBody> sendData(@Body Map<String, String> data);
}



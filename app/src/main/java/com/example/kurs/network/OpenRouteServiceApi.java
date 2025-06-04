package com.example.kurs.network;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;


public interface OpenRouteServiceApi {
    @Headers({
            "Content-Type: application/json",
            "Authorization: 5b3ce3597851110001cf624884b1ded505a84033aad7ea2edb718b95"
    })
    @POST("v2/directions/foot-walking/geojson")
    Call<ResponseBody> getRoute(@Body Map<String, Object> body);
    ;
}

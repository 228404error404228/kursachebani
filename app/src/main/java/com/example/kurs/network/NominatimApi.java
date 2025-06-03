package com.example.kurs.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.List;

public interface NominatimApi {
    @GET("search")
    Call<List<NominatimPlace>> searchPlaces(
            @Query("q") String query,
            @Query("format") String format,
            @Query("addressdetails") int addressDetails,
            @Query("limit") int limit,
            @Query("namedetails") int nameDetails,
            @Query("polygon_geojson") int polygon,
            @Query("extratags") int extraTags,
            @Query("email") String email,
            @Query("apikey") String apiKey
    );
}

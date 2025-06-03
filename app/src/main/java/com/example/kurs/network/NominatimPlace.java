package com.example.kurs.network;

public class NominatimPlace {
    private String display_name;
    private String lat;
    private String lon;

    public String getDisplayName() {
        return display_name;
    }

    public double getLat() {
        return Double.parseDouble(lat);
    }

    public double getLon() {
        return Double.parseDouble(lon);
    }
}


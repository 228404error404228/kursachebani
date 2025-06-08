package com.example.kurs.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


public class RouteViewModel extends ViewModel {
    private final MutableLiveData<Double> distance = new MutableLiveData<>();
    private final MutableLiveData<Double> duration = new MutableLiveData<>();

    public LiveData<Double> getDistance() {
        return distance;
    }

    public void setDistance(double dist) {
        distance.setValue(dist);
    }

    public LiveData<Double> getDuration() {
        return duration;
    }

    public void setDuration(double dur) {
        duration.setValue(dur);
    }
}

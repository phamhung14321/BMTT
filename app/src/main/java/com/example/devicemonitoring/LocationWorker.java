package com.example.devicemonitoring;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.devicemonitoring.LocationSender;



public class LocationWorker extends Worker {
    private Context context;

    public LocationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        LocationSender locationSender = new LocationSender(context);
        locationSender.getLocationAndSend();
        return Result.success();
    }
}

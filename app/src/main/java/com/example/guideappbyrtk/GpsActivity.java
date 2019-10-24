package com.example.guideappbyrtk;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class GpsActivity extends LocationCallback {
    private static final int LOCATION_REQUEST_CODE = 1;
    private Context context;
    public FusedLocationProviderClient fusedLocationProviderClient;
    private OnLocationResultListener mListener;
    GoogleApiClient googleApiClient;
    private static final String NORTH_POLE =
            "com.google.android.gms.location.sample.locationupdates" + ".NORTH_POLE";

    private static final float NORTH_POLE_LATITUDE = 90.0f;

    private static final float NORTH_POLE_LONGITUDE = 0.0f;

    private static final float ACCURACY_IN_METERS = 10.0f;


    public interface OnLocationResultListener {
        void onLocationResult(LocationResult locationResult);
    }

    public GpsActivity(Context context, OnLocationResultListener mListener) {
        this.context = context;
        this.mListener = mListener;
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @Override
    public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        mListener.onLocationResult(locationResult);
    }

    public void startLocationUpdates() {
        // パーミッションの確認
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Logger.d("Permission required.");
            ActivityCompat.requestPermissions((Activity) context, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,}, LOCATION_REQUEST_CODE);

            return;
        }

        // 端末の位置情報サービスが無効になっている場合、設定画面を表示して有効化を促す
        if (!isGPSEnabled()) {
            MapsActivity.gpsState.setText(R.string.gps_disable);
            showLocationSettingDialog();
            return;
        }

        MapsActivity.gpsState.setText(R.string.gps_enable);
        LocationRequest request = new LocationRequest();
        request.setFastestInterval(1000);
        request.setInterval(1000);
        request.setPriority(LocationRequest.PRIORITY_NO_POWER);

        fusedLocationProviderClient.requestLocationUpdates(request, this,null);
    }

    public void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(this);
    }

    private Boolean isGPSEnabled() {
        android.location.LocationManager locationManager = (android.location.LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    private void showLocationSettingDialog() {
        new android.app.AlertDialog.Builder(context)
                .setMessage("設定画面で位置情報サービスを有効にしてください")
                .setPositiveButton("設定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //NOP
                    }
                })
                .create()
                .show();
    }

    public void setMockLocation(){
        fusedLocationProviderClient.setMockMode(true);
        fusedLocationProviderClient.setMockLocation(createNorthPoleLocation());
    }

    public Location createNorthPoleLocation(){
        Location mockLocation = new Location(NORTH_POLE);
        mockLocation.setLatitude(NORTH_POLE_LATITUDE);
        mockLocation.setLongitude(NORTH_POLE_LONGITUDE);
        mockLocation.setAccuracy(ACCURACY_IN_METERS);
        mockLocation.setTime(System.currentTimeMillis());
        return mockLocation;
    }
}



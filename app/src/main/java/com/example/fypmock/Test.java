package com.example.fypmock;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import es.situm.sdk.SitumSdk;
import es.situm.sdk.error.Error;
import es.situm.sdk.location.LocationListener;
import es.situm.sdk.location.LocationManager;
import es.situm.sdk.location.LocationRequest;
import es.situm.sdk.location.LocationStatus;
import es.situm.sdk.model.location.Location;

public class Test extends GetBuildingID{
    LocationManager locationManager;
    LocationListener locationListener;
    String buildingid;
    ArrayList<String> selectedPois = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        //Initialize SitumSdk
        SitumSdk.init(this);

        // Set credentials
        SitumSdk.configuration().setUserPass("p19011503@student.newinti.edu.my", "T@nck2001");
        SitumSdk.configuration().setApiKey("p19011503@student.newinti.edu.my", "791bb3e3a8856145aed74aae9e138e8c1d45289fe7584b63c60ae60802c426c1");

        buildingid = getBuildingID();

        locationManager = SitumSdk.locationManager();

        //selectedPois.add("room2");
        //selectedPois.add("room3");
        selectedPois.add("KFC");
        selectedPois.add("Kyo Chon");
        selectedPois.add("Age's Ago");
    }

    @Override
    public void onResume(){
        checkPermissions();
        super.onResume();
    }

    public void map(View view) {
        Intent myIntent = new Intent(Test.this, Map.class);
        Test.this.startActivity(myIntent);
    }

    public void nav(View view) {
        if (!locationManager.isRunning()){
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    Location current = location; //store current location of user in current variable
                    SitumSdk.locationManager().removeUpdates(locationListener);

                    Log.d("location", "success");
                    Intent myIntent = new Intent(Test.this, Navigation.class);
                    myIntent.putExtra("USER_LOCATION", current);
                    myIntent.putExtra("SELECTED_POIS", selectedPois);

                    Test.this.startActivity(myIntent);
                }

                @Override
                public void onStatusChanged(@NonNull LocationStatus locationStatus) {
                    Log.d("TEST", "onStatusChanged(): " + locationStatus);
                }

                @Override
                public void onError(@NonNull Error error) {
                    Log.e("TEST", "onError(): " + error.getMessage());
                }

            };

            LocationRequest locationRequest = new LocationRequest.Builder()
                    .buildingIdentifier(buildingid)
                    .useDeadReckoning(true)
                    .build();

            SitumSdk.locationManager().requestLocationUpdates(locationRequest, locationListener);

        }else{
            SitumSdk.locationManager().removeUpdates(locationListener);
        }
    }

    private void requestPermissions(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(Test.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    2209);
        }else {
            ActivityCompat.requestPermissions(Test.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    3096);
        }

    }

    private void checkPermissions() {
        boolean hasFineLocationPermission = ContextCompat.checkSelfPermission(Test.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            boolean hasBluetoothScanPermission = ContextCompat.checkSelfPermission(Test.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean hasBluetoothConnectPermission = ContextCompat.checkSelfPermission(Test.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

            if(!hasBluetoothConnectPermission || !hasBluetoothScanPermission || !hasFineLocationPermission){
                if (ActivityCompat.shouldShowRequestPermissionRationale(Test.this, Manifest.permission.BLUETOOTH_SCAN)
                        || ActivityCompat.shouldShowRequestPermissionRationale(Test.this, Manifest.permission.BLUETOOTH_CONNECT)
                        || ActivityCompat.shouldShowRequestPermissionRationale(Test.this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                    Snackbar.make(findViewById(android.R.id.content),
                            "Need bluetooth or location permission to enable service",
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction("Open", view -> requestPermissions()).show();
                } else {
                    requestPermissions();
                }
            }
        }else {

            if(!hasFineLocationPermission){
                if (ActivityCompat.shouldShowRequestPermissionRationale(Test.this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                    Snackbar.make(findViewById(android.R.id.content),
                            "Need location permission to enable service",
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction("Open", view -> requestPermissions()).show();
                } else {
                    requestPermissions();
                }
            }
        }
    }

    /**
     *
     * REQUESTCODE = 1 : NO PERMISSIONS
     *
     */

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 3096) {
            if (!(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finishActivity(requestCode);
            }
        }else if(requestCode == 2209){
            if (!(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finishActivity(requestCode);
            }
            if (!(grantResults.length > 1
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                finishActivity(requestCode);
            }
        }

    }
}
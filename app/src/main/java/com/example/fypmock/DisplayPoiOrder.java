package com.example.fypmock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.situm.sdk.SitumSdk;
import es.situm.sdk.directions.DirectionsRequest;
import es.situm.sdk.error.Error;
import es.situm.sdk.location.LocationListener;
import es.situm.sdk.location.LocationManager;
import es.situm.sdk.location.LocationRequest;
import es.situm.sdk.location.LocationStatus;
import es.situm.sdk.model.cartography.Poi;
import es.situm.sdk.model.cartography.Point;
import es.situm.sdk.model.directions.Route;
import es.situm.sdk.model.location.Location;
import es.situm.sdk.utils.Handler;

public class DisplayPoiOrder extends GetBuildingID {

    LocationManager locationManager;
    LocationListener locationListener;

    private String buildingID;
    private final GetPoiUseCase getPoiUseCase = new GetPoiUseCase();
    private final List<Poi> buildingPoi = new ArrayList<>();

    private Location firstLocation;
    private Point startingPoint;
    private String navMethod;
    private ArrayList<String> selectedPoiList = new ArrayList<>();
    private final ArrayList<String[]> allCombinations = new ArrayList<>();
    private ArrayList<String[]> pairPoi = new ArrayList<>();
    private ArrayList<Double> pairPoiDistance = new ArrayList<>();
    private ArrayList<String[]> samePaths = new ArrayList<>();
    private ArrayList<String> orderToDisplay = new ArrayList<>();
    private boolean calNextFloor = false;
    private boolean nextFloorCalculated = false;
    private DisplayPoiAdapter adapter;
    private ListView mlv_fav;
    private ProgressBar mProgressBar;
    private LinearLayout mButtonLayout;
    private Button navigation_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_poi_order);

        navMethod = (String) getIntent().getStringExtra("SELECTED_METHOD");
        selectedPoiList.addAll(getIntent().getParcelableArrayListExtra("SELECTED_POIS"));

        mProgressBar = findViewById(R.id.progressBar);
        mlv_fav = findViewById(R.id.lv_poiOrder);
        Button addMorePoi_btn = findViewById(R.id.btn_addMorePoi);
        navigation_btn = findViewById(R.id.btn_navigation);
        mButtonLayout = findViewById(R.id.button_layout);

        mProgressBar.setVisibility(View.VISIBLE);

        addMorePoi_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        buildingID = getBuildingID();

        //Initialize SitumSdk
        SitumSdk.init(this);

        // Set credentials
        SitumSdk.configuration().setUserPass("p19011503@student.newinti.edu.my", "T@nck2001");
        SitumSdk.configuration().setApiKey("p19011503@student.newinti.edu.my", "791bb3e3a8856145aed74aae9e138e8c1d45289fe7584b63c60ae60802c426c1");

        locationManager = SitumSdk.locationManager();

        switch (navMethod){
            case "Follow sequence":
                if (!locationManager.isRunning()){
                    locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            SitumSdk.locationManager().removeUpdates(locationListener);

                            firstLocation = location;
                            orderToDisplay.clear();
                            orderToDisplay.addAll(selectedPoiList);
                            adapter = new DisplayPoiAdapter(DisplayPoiOrder.this, R.layout.item, orderToDisplay);
                            mlv_fav.setAdapter(adapter);

                            mButtonLayout.setVisibility(View.VISIBLE);
                            navigation_btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(DisplayPoiOrder.this, Navigation.class);
                                    intent.putExtra("USER_LOCATION", firstLocation);
                                    intent.putExtra("SELECTED_POIS", orderToDisplay);
                                    DisplayPoiOrder.this.startActivity(intent);
                                }
                            });

                            mProgressBar.setVisibility(View.GONE);
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
                            .buildingIdentifier(buildingID)
                            .useDeadReckoning(true)
                            .build();

                    SitumSdk.locationManager().requestLocationUpdates(locationRequest, locationListener);

                }else{
                    SitumSdk.locationManager().removeUpdates(locationListener);
                }

                break;

            case "Follow sequence + return to start":
                if (!locationManager.isRunning()){
                    locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            SitumSdk.locationManager().removeUpdates(locationListener);

                            firstLocation = location;
                            orderToDisplay.clear();
                            orderToDisplay.addAll(selectedPoiList);
                            orderToDisplay.add("Starting Point");
                            adapter = new DisplayPoiAdapter(DisplayPoiOrder.this, R.layout.item, orderToDisplay);
                            mlv_fav.setAdapter(adapter);
                            mProgressBar.setVisibility(View.GONE);
                            mButtonLayout.setVisibility(View.VISIBLE);

                            navigation_btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(DisplayPoiOrder.this, Navigation.class);
                                    intent.putExtra("USER_LOCATION", firstLocation);
                                    intent.putExtra("SELECTED_POIS", orderToDisplay);
                                    DisplayPoiOrder.this.startActivity(intent);
                                }
                            });

                            mProgressBar.setVisibility(View.GONE);
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
                            .buildingIdentifier(buildingID)
                            .useDeadReckoning(true)
                            .build();

                    SitumSdk.locationManager().requestLocationUpdates(locationRequest, locationListener);

                }else{
                    SitumSdk.locationManager().removeUpdates(locationListener);
                }

                break;

            case "Shortest path":
            case "Shortest path + return to start":
                getAllCombinations(selectedPoiList, 0);
                setPoiOfBuilding();
                break;
        }
    }

    @Override
    protected void onResume() {
        checkPermissions();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (!locationManager.isRunning()){
            return;
        }

        locationManager.removeUpdates(locationListener);
        SitumSdk.locationManager().removeUpdates(locationListener);

        super.onDestroy();
    }

    private class DisplayPoiAdapter extends ArrayAdapter<String> {
        private int layout;

        public DisplayPoiAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
            layout = resource;
        }

        @SuppressLint("SetTextI18n")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            DisplayPoiOrder.ViewHolder mainViewholder = null;

            if (convertView == null){
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                DisplayPoiOrder.ViewHolder viewHolder = new ViewHolder();
                viewHolder.poiName = (TextView) convertView.findViewById(R.id.tv_poiName);
                viewHolder.fav = (ImageView) convertView.findViewById(R.id.fav);
                viewHolder.unfav = (ImageView) convertView.findViewById(R.id.unfav);

                convertView.setTag(viewHolder);
                mainViewholder = (DisplayPoiOrder.ViewHolder) viewHolder;
            }else
                mainViewholder = (DisplayPoiOrder.ViewHolder) convertView.getTag();

            mainViewholder.poiName.setText((position + 1) + ". " + getItem(position));
            mainViewholder.fav.setVisibility(View.GONE);
            mainViewholder.unfav.setVisibility(View.GONE);

            return convertView;
        }
    }

    public class ViewHolder {
        TextView poiName;
        ImageView fav, unfav;
    }

    private void getShortestPath(){
        String fromName = "", toName = "";
        int combCounter = 0;
        double shortestDistance = 0;
        ArrayList<String> shortestPath = new ArrayList<>();

        for (String[] combination : allCombinations){
            double totalDistance = 0;

            for (int i=0; i<combination.length; i++){
                if (i==0){
                    //start location
                    fromName = "Starting Point";
                }else{
                    fromName = combination[i-1];
                }

                toName = combination[i];

                for (int x=0; x<pairPoi.size(); x++){
                    String[] pair = pairPoi.get(x);

                    if ((pair[0].equals(fromName) && pair[1].equals(toName))
                            || ((pair[1].equals(fromName) && pair[0].equals(toName)))){
                        totalDistance += pairPoiDistance.get(x);
                        break;
                    }
                }
            }

            if (shortestDistance == 0){
                shortestDistance = totalDistance;
                //add start location
                shortestPath.clear();
                shortestPath.addAll(Arrays.asList(allCombinations.get(combCounter)));
                String[] temp = shortestPath.toArray(new String[0]);
                samePaths.add(temp);
            }else{
                if (totalDistance < shortestDistance){
                    samePaths.clear();
                    shortestDistance = totalDistance;
                    //add start location
                    shortestPath.clear();
                    shortestPath.addAll(Arrays.asList(allCombinations.get(combCounter)));
                    String[] temp = shortestPath.toArray(new String[0]);
                    samePaths.add(temp);
                }else if (totalDistance == shortestDistance){
                    String[] temp = shortestPath.toArray(new String[0]);
                    samePaths.add(temp);
                }
            }

            combCounter++;
        }


        orderToDisplay.clear();
        orderToDisplay.addAll(shortestPath);
        adapter = new DisplayPoiAdapter(this, R.layout.item, orderToDisplay);
        mlv_fav.setAdapter(adapter);
        mProgressBar.setVisibility(View.GONE);

        navigation_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DisplayPoiOrder.this, Navigation.class);
                intent.putExtra("USER_LOCATION", firstLocation);
                intent.putExtra("SELECTED_POIS", orderToDisplay);
                DisplayPoiOrder.this.startActivity(intent);
            }
        });
        mButtonLayout.setVisibility(View.VISIBLE);
    }

    private void getPairDistance(int cid){
        String fromName, toName;
        Point from = null, to = null;

        if (cid == pairPoi.size()){
            getShortestPath();
            return;
        }

        String[] pair = pairPoi.get(cid);
        fromName = pair[0];
        toName = pair[1];

        for(Poi poi : buildingPoi){
            if (poi.getName().equals(fromName)){
                from = poi.getPosition();
            }else if(poi.getName().equals(toName)){
                to = poi.getPosition();
            }
        }

        if (fromName.equals("Starting Point")){
            from = startingPoint;
        }else if (toName.equals("Starting Point")){
            to = startingPoint;
        }

        if (!from.getFloorIdentifier().equals(to.getFloorIdentifier())){
            if (calNextFloor){
                nextFloorCalculated = true;

                for(Poi poi : buildingPoi){
                    if (poi.getName().equals("Stair") && poi.getFloorIdentifier().equals(to.getFloorIdentifier())){
                        from = poi.getPosition();
                    }
                }
            }else{
                calNextFloor = true;

                for(Poi poi : buildingPoi){
                    if (poi.getName().equals("Stair") && poi.getFloorIdentifier().equals(from.getFloorIdentifier())){
                        to = poi.getPosition();
                    }
                }
            }
        }

        DirectionsRequest directionsRequest = new DirectionsRequest.Builder()
                .from(from, null)
                .to(to)
                .build();

        SitumSdk.directionsManager().requestDirections(directionsRequest, new Handler<Route>() {
            @Override
            public void onSuccess(Route route) {
                if (calNextFloor){
                    if (!nextFloorCalculated){
                        pairPoiDistance.add(route.getDistance());
                        getPairDistance(cid);
                    }else{
                        double prevDist = pairPoiDistance.get(cid);

                        pairPoiDistance.set(cid, prevDist + route.getDistance());
                        nextFloorCalculated = false;
                        calNextFloor = false;
                        getPairDistance(cid + 1);
                    }

                }else{
                    pairPoiDistance.add(route.getDistance());
                    getPairDistance(cid + 1);
                }
            }

            @Override
            public void onFailure(Error error) {
                Log.e("DisplayPoiOrder", error.getMessage());
            }
        });
    }

    private boolean checkPairExist(String from, String to){
        for (String[] pair : pairPoi) {
            if ((pair[0].equals(from) && pair[1].equals(to)) || ((pair[1].equals(from) && pair[0].equals(to)))) {
                return true;
            }
        }

        return false;
    }

    private void formPoiPairs(){
        int currentIteration = 1;

        for (String[] combination : allCombinations) {
            Log.d("TAG", "iteration: " + currentIteration);

            for (int x = 0; x < combination.length; x++) {
                String fromName = "", toName = "";

                //get from name
                if (x == 0) {
                    //modify
                    fromName = "Starting Point";
                    Log.d("TAG", "from: Starting Point");
                } else {
                    for (Poi poi : buildingPoi) {
                        if (poi.getName().equals(combination[x - 1])) {
                            fromName = poi.getName();
                            Log.d("TAG", "from: " + poi.getName());
                        }
                    }
                }

                if (combination[x].equals("Starting Point")){
                    toName = "Starting Point";
                    Log.d("TAG", "to: Starting Point");
                }else{
                    //get to position
                    for (Poi poi : buildingPoi) {
                        if (poi.getName().equals(combination[x])) {
                            toName = poi.getName();
                            Log.d("TAG", "to: " + poi.getName());
                        }
                    }
                }

                boolean pairExists = checkPairExist(fromName, toName);

                if (!pairExists) {
                    String[] temp = {fromName, toName};
                    pairPoi.add(temp);
                    Log.d("TAG", "unique pair: " + Arrays.toString(temp));
                }
            }
            currentIteration++;
        }

        getPairDistance(0);
    }

    private void setCurrentLocation(){
        if (!locationManager.isRunning()){
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    SitumSdk.locationManager().removeUpdates(locationListener);

                    startingPoint = location.getPosition();
                    firstLocation = location;
                    formPoiPairs();
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
                    .buildingIdentifier(buildingID)
                    .useDeadReckoning(true)
                    .build();

            SitumSdk.locationManager().requestLocationUpdates(locationRequest, locationListener);

        }else{
            SitumSdk.locationManager().removeUpdates(locationListener);
        }
    }

    private void setPoiOfBuilding(){
        getPoiUseCase.get(buildingID, new GetPoiUseCase.Callback() {
            @Override
            public void onSuccess(List<Poi> pois) {
                buildingPoi.addAll(pois);

                setCurrentLocation();
            }

            @Override
            public void onError(Error error) {
                Toast.makeText(DisplayPoiOrder.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void swapPosition(ArrayList<String> selectedPoiList, int i, int j){
        String temp = selectedPoiList.get(i);
        selectedPoiList.set(i, selectedPoiList.get(j));
        selectedPoiList.set(j, temp);
    }

    private void getAllCombinations(ArrayList<String> selectedPoiList, int cid){
        if (cid == selectedPoiList.size()-1){
            String[] temp;

            if(navMethod.equals("Shortest path + return to start")){
                ArrayList<String> tempArrList = new ArrayList<>(selectedPoiList);
                tempArrList.add("Starting Point");
                temp = tempArrList.toArray(new String[0]);
            }else{
                temp = selectedPoiList.toArray(new String[0]);
            }

            allCombinations.add(temp);
            return;
        }

        for (int i=cid; i<selectedPoiList.size(); i++){
            swapPosition(selectedPoiList, i, cid);
            getAllCombinations(selectedPoiList, cid+1);
            swapPosition(selectedPoiList, i, cid);
        }
    }

    private void requestPermissions(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(DisplayPoiOrder.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    2209);
        }else {
            ActivityCompat.requestPermissions(DisplayPoiOrder.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    3096);
        }

    }

    private void checkPermissions() {
        boolean hasFineLocationPermission = ContextCompat.checkSelfPermission(DisplayPoiOrder.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            boolean hasBluetoothScanPermission = ContextCompat.checkSelfPermission(DisplayPoiOrder.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean hasBluetoothConnectPermission = ContextCompat.checkSelfPermission(DisplayPoiOrder.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

            if(!hasBluetoothConnectPermission || !hasBluetoothScanPermission || !hasFineLocationPermission){
                if (ActivityCompat.shouldShowRequestPermissionRationale(DisplayPoiOrder.this, Manifest.permission.BLUETOOTH_SCAN)
                        || ActivityCompat.shouldShowRequestPermissionRationale(DisplayPoiOrder.this, Manifest.permission.BLUETOOTH_CONNECT)
                        || ActivityCompat.shouldShowRequestPermissionRationale(DisplayPoiOrder.this, Manifest.permission.ACCESS_FINE_LOCATION)) {

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
                if (ActivityCompat.shouldShowRequestPermissionRationale(DisplayPoiOrder.this, Manifest.permission.ACCESS_FINE_LOCATION)) {

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
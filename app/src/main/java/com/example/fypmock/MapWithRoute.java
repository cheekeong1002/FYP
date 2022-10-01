package com.example.fypmock;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import es.situm.sdk.SitumSdk;
import es.situm.sdk.error.Error;
import es.situm.sdk.model.cartography.Building;
import es.situm.sdk.model.cartography.Poi;
import es.situm.sdk.model.cartography.Point;
import es.situm.sdk.utils.Handler;

public class MapWithRoute extends GetBuildingID
        implements OnMapReadyCallback {

    private static final String TAG = Map.class.getSimpleName();
    private final GetPoiUseCase getPoiUseCase = new GetPoiUseCase();

    private String buildingId;
    private Building selectedBuilding;
    private ArrayList<String> poiNamesList = new ArrayList<>();
    private ArrayList<Poi> poiList = new ArrayList<>();
    private final List<Poi> buildingPoi = new ArrayList<>();
    private GoogleMap map;
    private FloorSelectorView floorSelectorView;
    private Point startingPoint;
    private boolean multipleFloor = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_with_route);

        poiNamesList.addAll(getIntent().getStringArrayListExtra("SELECTED_POI"));
        startingPoint = (Point) getIntent().getSerializableExtra("STARTING_POINT");
        Toast.makeText(this, "" + startingPoint, Toast.LENGTH_SHORT).show();

        Button endPreview_btn = findViewById(R.id.btn_endPreview);
        endPreview_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //Initialize SitumSdk
        SitumSdk.init(this);

        // Set credentials
        SitumSdk.configuration().setUserPass("p19011503@student.newinti.edu.my", "T@nck2001");
        SitumSdk.configuration().setApiKey("p19011503@student.newinti.edu.my", "791bb3e3a8856145aed74aae9e138e8c1d45289fe7584b63c60ae60802c426c1");

        buildingId = getBuildingID(); //get the building ID to be used

        //Get all the buildings of the account
        SitumSdk.communicationManager().fetchBuildings(new Handler<Collection<Building>>() {
            @Override
            public void onSuccess(Collection<Building> buildings) {
                Log.d(TAG, "onSuccess: Your buildings: ");
                for (Building building : buildings) {
                    Log.i(TAG, "onSuccess: " + building.getIdentifier() + " - " + building.getName());

                    //check if building id in account matches building id to be used
                    if (buildingId.equals(building.getIdentifier())) {
                        selectedBuilding = building; //store the building object from account
                        setFragment(); //call function to start fragment
                        return;
                    }
                }

                if (buildings.isEmpty()) {
                    Log.e(TAG, "onSuccess: you have no buildings. Create one in the Dashboard");
                }
            }

            @Override
            public void onFailure(Error error) {
                Log.e(TAG, "onFailure:" + error);
            }
        });


    }

    protected void setFragment(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if(mapFragment != null){
            mapFragment.getMapAsync(this);
        }
    }

    private void plotMarker(){
        Bitmap destinationScaled;
        Bitmap bitmapDestination = BitmapFactory.decodeResource(getResources(), R.drawable.destination);
        destinationScaled = Bitmap.createScaledBitmap(bitmapDestination, bitmapDestination.getWidth() / 10,bitmapDestination.getHeight() / 10, false);

        if (multipleFloor){

        }else{
            if (floorSelectorView.getSelectedFloorId().equals(startingPoint.getFloorIdentifier())){
                for (Poi poi : poiList){

                    LatLng destinationLatLng = new LatLng(poi.getCoordinate().getLatitude(),
                            poi.getCoordinate().getLongitude());

                    //add marker to indicate destination on Google map
                    map.addMarker(new MarkerOptions()
                        .position(destinationLatLng)
                        .zIndex(90) //provide high index so that this marker will be drawn over other markers
                        .flat(true) //set to flat to rotate or tilt with camera
                        .anchor(0.5f,0.8f) //point of image that will be placed at latlong position of marker
                        .icon(BitmapDescriptorFactory.fromBitmap(destinationScaled)));
                }
            }
        }
    }

    private void checkFloorDiff(){
        //store all poi to be navigated in an array list
        for (String poiName : poiNamesList){
            for (Poi poi : buildingPoi){
                if (poi.getName().equals(poiName)){
                    poiList.add(poi);
                    break;
                }
            }
        }

        //check if pois and starting point are in different floor
        String floorId = "";
        for (Poi poi : poiList){
            if (floorId.equals("")){
                floorId = poi.getFloorIdentifier();
            }else{
                if (!floorId.equals(poi.getFloorIdentifier())){
                    multipleFloor = true;
                    break;
                }
            }
        }

        if (!startingPoint.getFloorIdentifier().equals(floorId)){
            multipleFloor = true;
        }

        Toast.makeText(this, "" + floorId + ", " + startingPoint.getFloorIdentifier(), Toast.LENGTH_LONG).show();

        plotMarker();
    }

    private void setMap(){
        getPoiUseCase.get(buildingId, new GetPoiUseCase.Callback() {
            @Override
            public void onSuccess(List<Poi> pois) {
                buildingPoi.addAll(pois);
                checkFloorDiff();
            }

            @Override
            public void onError(Error error) {
                Toast.makeText(MapWithRoute.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Method triggered after the map is ready
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        floorSelectorView = findViewById(R.id.situm_floor_selector);
        floorSelectorView.setFloorSelector(selectedBuilding, googleMap, false);
        setMap();
    }

}

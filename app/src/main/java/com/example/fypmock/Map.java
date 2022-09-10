 package com.example.fypmock;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.Collection;

import es.situm.sdk.SitumSdk;
import es.situm.sdk.error.Error;
import es.situm.sdk.model.cartography.Building;
import es.situm.sdk.utils.Handler;

public class Map
        extends GetBuildingID
        implements OnMapReadyCallback {

    private static final String TAG = Map.class.getSimpleName();

    private String buildingId;
    private Building selectedBuilding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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

    /**
     * Method triggered after the map is ready
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        FloorSelectorView floorSelectorView = findViewById(R.id.situm_floor_selector);
        floorSelectorView.setFloorSelector(selectedBuilding, googleMap, false);
    }

}

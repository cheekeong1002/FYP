package com.example.fypmock;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import es.situm.sdk.SitumSdk;
import es.situm.sdk.error.Error;
import es.situm.sdk.model.cartography.Building;
import es.situm.sdk.model.cartography.Floor;
import es.situm.sdk.model.location.Bounds;
import es.situm.sdk.model.location.Coordinate;
import es.situm.sdk.utils.Handler;

public class FloorSelectorView extends ConstraintLayout {

    private Building building;
    private GoogleMap map;
    private GroundOverlay groundOverlay;

    private FloorAdapter floorAdapter;
    private List<Floor> floorList;
    private Floor lastFloorSelected;

    private RecyclerView selector;

    private boolean isFirstCameraAnimation = true;
    private boolean focusUserMarker = true;
    private boolean setDestination = false;

    public FloorSelectorView(Context context) {
        super(context);
        setup();
    }

    public FloorSelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    /**
     * Instances the FloorSelector components
     */
    private void setup(){
        //Obtain LayoutInflater from currently running context and inflate layout into current ViewGroup
        LayoutInflater.from(getContext()).inflate(R.layout.situm_level_list, this);

        selector = findViewById(R.id.recycler_level_list);

        //set layout manager for recycler view
        selector.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    }

    /**
     * Reset the RecyclerView
     */
    public void reset(){
        floorAdapter.positioningFloorChangedTo(null, false);
        lastFloorSelected = floorAdapter.getSelected();
        isFirstCameraAnimation = true;
        fetchFloorsFromBuilding();
    }

    /**
     * Sets the GoogleMap and Building
     */
    public void setFloorSelector(Building building, GoogleMap map, boolean setDest) {
        this.building = building;
        this.map = map;
        setDestination = setDest;
        fetchFloorsFromBuilding();
    }

    /**
     * Iterates the collection building's floors with the method fetchFloorsFromBuilding() of SitumSdk, stores them in reverse order.
     */
    private void fetchFloorsFromBuilding() {

        SitumSdk.communicationManager().fetchFloorsFromBuilding(building.getIdentifier(), new Handler<Collection<Floor>>() {
            @Override
            public void onSuccess(Collection<Floor> floorsCollection) {

                //store retrieved floor details in reverse so that 1st floor will always be at the bottom of list
                floorList = new ArrayList<>(floorsCollection);
                Collections.reverse(floorList);

                prepareSelector();
            }

            @Override
            public void onFailure(Error error) {
                onError(error);
            }

        });

    }

    /**
     * Prepares the FloorSelectorView with its adapter and selects by default the last floor.
     */
    private void prepareSelector() {
        floorAdapter = new FloorAdapter(floorList, this::onSelectFloor, getContext());

        //set the adapter to populate RecyclerView with data
        selector.setAdapter(floorAdapter);

        // The next method selects by default the first item of the List<>
        // If the user is leaving positioning mode, sets the default floor to the last selected one by the user
        Floor defaultFloor = lastFloorSelected != null?lastFloorSelected:floorList.get(floorList.size() - 1);
        onSelectFloor(defaultFloor);

    }

    /**
     * Listener of the RecyclerView, that draws the respective floor and indicates it on the selector.
     */
    private void onSelectFloor(Floor newFloor) {

        if(!isFirstCameraAnimation)
            focusUserMarker = false;
        else
            selector.scrollToPosition(floorList.indexOf(newFloor));

        if (floorAdapter.getSelected() != null && floorAdapter.getSelected().equals(newFloor))
            return;

        SitumSdk.communicationManager().fetchMapFromFloor(newFloor, new Handler<Bitmap>() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                drawFloor(bitmap);
                floorAdapter.select(newFloor);
                Log.d("test", "set Destination: " + setDestination);
                if (setDestination){
                    Navigation.floorChanged();
                }
            }

            @Override
            public void onFailure(Error error) {
                onError(error);
            }
        });

    }

    /**
     * Receives a floor plan to paint it inside the building bounds.
     */
    private void drawFloor(Bitmap bitmap) {
        Bounds drawBounds = building.getBounds();
        Coordinate coordinateNE = drawBounds.getNorthEast();
        Coordinate coordinateSW = drawBounds.getSouthWest();
        LatLngBounds latLngBounds = new LatLngBounds(
                new LatLng(coordinateSW.getLatitude(), coordinateSW.getLongitude()),
                new LatLng(coordinateNE.getLatitude(), coordinateNE.getLongitude()));

        //remove any existing image overlay
        if (groundOverlay != null) {
            groundOverlay.remove();
        }

        groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                .bearing((float) building.getRotation().degrees())
                .positionFromBounds(latLngBounds));

        if(isFirstCameraAnimation){
            //add animation to slowly zoom into map to view floor plan
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
            isFirstCameraAnimation = false;
        }
    }

    private void onError(Error error) {
        Log.e("onError()", error.getMessage());
        Snackbar.make(findViewById(R.id.container), error.getMessage(), Snackbar.LENGTH_INDEFINITE).show();
    }

    /**
     * Searches the new floor positioning in and indicate the change in the selector
     *
     * @param positioningFloorId String
     */
    public void updatePositioningFloor(String positioningFloorId) {
        Floor newFloor = searchFloorById(positioningFloorId);

        SitumSdk.communicationManager().fetchMapFromFloor(newFloor, new Handler<Bitmap>() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                //draw current floor on Google map if this is the first received location
                if(focusUserMarker) {
                    drawFloor(bitmap);
                    selector.scrollToPosition(floorList.indexOf(newFloor));
                }

                floorAdapter.positioningFloorChangedTo(newFloor, focusUserMarker);
                if (setDestination){
                    Navigation.floorChanged();
                }
            }

            @Override
            public void onFailure(Error error) {
                onError(error);
            }
        });
    }

    /**
     * Finds the floor by the Identifier provided in the parameters
     *
     * @param floorId Identifier of the floor we want to get
     * @return Floor
     */
    private Floor searchFloorById(String floorId) {
        Floor floorFound = null;

        for (Floor f:floorList) {
            if(f.getIdentifier().equals(floorId)){
                floorFound = f;
            }
        }

        return floorFound;
    }

    public String getSelectedFloorId() {
        return floorAdapter.getSelected() != null ? floorAdapter.getSelected().getIdentifier() : null;
    }

    public boolean focusUserMarker() {
        return focusUserMarker;
    }

    public void setFocusUserMarker(boolean state) {
        focusUserMarker = state;
    }

}

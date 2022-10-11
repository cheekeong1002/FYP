package com.example.fypmock;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import es.situm.sdk.model.directions.Route;
import es.situm.sdk.model.directions.RouteSegment;
import es.situm.sdk.model.cartography.Point;

import java.util.ArrayList;
import java.util.List;

import es.situm.sdk.SitumSdk;
import es.situm.sdk.directions.DirectionsRequest;
import es.situm.sdk.error.Error;
import es.situm.sdk.location.LocationListener;
import es.situm.sdk.location.LocationManager;
import es.situm.sdk.location.LocationRequest;
import es.situm.sdk.location.LocationStatus;
import es.situm.sdk.model.cartography.Building;
import es.situm.sdk.model.cartography.Floor;
import es.situm.sdk.model.cartography.Poi;
import es.situm.sdk.model.location.Location;
import es.situm.sdk.model.navigation.NavigationProgress;
import es.situm.sdk.navigation.NavigationListener;
import es.situm.sdk.navigation.NavigationRequest;
import es.situm.sdk.utils.Handler;

public class Navigation extends GetBuildingID implements OnMapReadyCallback {
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 3096;
    private static final int LOCATION_BLUETOOTH_REQUEST_CODE = 2209;
    private static final String TAG = Navigation.class.getSimpleName();

    private static final int UPDATE_LOCATION_ANIMATION_TIME = 600;
    private static final int MIN_CHANGE_IN_BEARING_TO_ANIMATE_CAMERA = 10;

    private static final String FIRST_FLOOR_ID = "34489";
    private static final String SECOND_FLOOR_ID = "34490";

    private static GoogleMap map;
    private Marker marker;
    private static Marker destinationMarker;
    private static Marker stairMarker;
    private static Marker initialLocMarker;
    private static FloorSelectorView floorSelectorView;

    private final GetBuildingUseCase getBuildingUseCase = new GetBuildingUseCase();
    private final GetPoiUseCase getPoiUseCase = new GetPoiUseCase();

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location current;
    private String buildingId;
    private Building building;
    private GroundOverlay groundOverlay;

    private boolean markerWithOrientation = false;
    private static boolean firstPreview = true;
    private LatLng lastCameraLatLng;
    private float lastCameraBearing;
    private static String initialPositioningFloorId;
    private static String lastPositioningFloorId;
    private static Location firstLocation;
    private Location lastLocation;

    private static ArrayList<String> selectedPoiNames = new ArrayList<>();
    private static final List<Poi> buildingPoi = new ArrayList<>();
    private static int currentPoiPost = 1;
    private static Poi destinationPoi;
    private static int floorSpan;
    private static String currentSelectedFloorID = FIRST_FLOOR_ID; //first floor selected by default
    private static String lastSelectedFloorID;

    private Button startNavBtn;
    private NavigationRequest navReq;
    private static boolean navStarted = false;
    private static List<Polyline> routeF1 = new ArrayList<>();
    private static List<Polyline> routeF2 = new ArrayList<>();
    private static Route lastF1Route;
    private static Route lastF2Route;
    private RelativeLayout navDescLayout;
    private TextView mtvDirection;
    private static boolean pauseNav = false;
    private static boolean waitPostFloorChg = false;
    private static boolean floorChanged = false;
    private int outRouteCounter = 0;
    private boolean recalculatingRoute = false;
    private static Route recalculatedF1;
    private static Route recalculatedF2;

    PositionAnimator positionAnimator = new PositionAnimator();

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        firstLocation = (Location) getIntent().getParcelableExtra("USER_LOCATION");
        selectedPoiNames = getIntent().getStringArrayListExtra("SELECTED_POIS");
        Log.d(TAG, "first location: " + firstLocation);
        Log.d(TAG, "array: " + selectedPoiNames);
        navDescLayout = (RelativeLayout) findViewById(R.id.nav_layout);
        mtvDirection = (TextView) findViewById(R.id.tvDirection);
        startNavBtn = findViewById(R.id.start_button);

        //Initialize SitumSdk
        SitumSdk.init(this);

        // Set credentials
        SitumSdk.configuration().setUserPass("p19011503@student.newinti.edu.my", "T@nck2001");
        SitumSdk.configuration().setApiKey("p19011503@student.newinti.edu.my", "791bb3e3a8856145aed74aae9e138e8c1d45289fe7584b63c60ae60802c426c1");

        locationManager = SitumSdk.locationManager();

        buildingId = getBuildingID();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if(mapFragment != null){
            mapFragment.getMapAsync(this); //initialize map system and view
        }

        setup();
    }

    @Override
    public void onResume(){
        checkPermissions();
        super.onResume();
    }

    @Override
    public void onDestroy(){
        reset(true);
        super.onDestroy();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        getBuildingUseCase.get(buildingId, new GetBuildingUseCase.Callback() {
            @Override
            public void onSuccess(Building build, Floor floor, Bitmap bitmap) {
                progressBar.setVisibility(View.GONE);
                building = build;

                //instance a new FloorSelector after getting building and Google map
                floorSelectorView = findViewById(R.id.situm_floor_selector);
                floorSelectorView.setFloorSelector(building, map, true);
            }

            @Override
            public void onError(Error error) {
                Toast.makeText(Navigation.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }

        });

    }

    private void reset(boolean resetAll){
        progressBar.setVisibility(ProgressBar.GONE);
        floorSelectorView.reset();
        lastPositioningFloorId = null;
        stopLocation();

        buildingPoi.clear();
        firstPreview = true;
        removeMarker();
        removeRoute();
        pauseNav = false;
        lastF1Route = null;
        lastF2Route = null;
        waitPostFloorChg = false;
        floorChanged = false;
        recalculatingRoute = false;
        outRouteCounter = 0;
        recalculatedF1 = null;
        recalculatedF2 = null;

        if (resetAll){
            Log.d(TAG, "resetting all");
            getBuildingUseCase.cancel();
            getPoiUseCase.cancel();
            currentPoiPost = 1;
            lastLocation = null;
            selectedPoiNames = null;
        }
    }

    private void setup(){
        Log.d(TAG, "setup: " + currentPoiPost);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.GONE);
        startNavBtn.setVisibility(View.VISIBLE);
        FloatingActionButton button2 = findViewById(R.id.button2);

        //set the initial positioning floor id
        if (currentPoiPost == 1){
            initialPositioningFloorId = firstLocation.getFloorIdentifier();
        }else{
            for (Poi x : buildingPoi){
                if (x.getName().equals(selectedPoiNames.get(currentPoiPost - 2))){
                    initialPositioningFloorId = x.getFloorIdentifier();
                    break;
                }
            }
        }

        //get all poi from building
        getPoiUseCase.get(buildingId, new GetPoiUseCase.Callback() {
            @Override
            public void onSuccess(List<Poi> pois) {
                buildingPoi.addAll(pois);

                if (selectedPoiNames.get(currentPoiPost - 1).equals("Starting Point")){
                    destinationPoi = null;

                    if (initialPositioningFloorId.equals(firstLocation.getFloorIdentifier())){
                        floorSpan = 1;
                    }else{
                        floorSpan = 2;
                    }

                    plotMarkers();
                }else{
                    for (Poi poi : buildingPoi){
                        if (poi.getName().equals(selectedPoiNames.get(currentPoiPost - 1))){
                            destinationPoi = poi; //get poi of current destination

                            if (initialPositioningFloorId.equals(destinationPoi.getFloorIdentifier())){
                                floorSpan = 1;
                            }else{
                                floorSpan = 2;
                            }

                            plotMarkers();

                            break;
                        }
                    }
                }


            }

            @Override
            public void onError(Error error) {
                Toast.makeText(Navigation.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        View.OnClickListener buttonListenerLocation = view -> {
            if(!locationManager.isRunning()){
                //set marker orientation to false by default
                markerWithOrientation = false;
                //set to true to select the floor being positioned in
                floorSelectorView.setFocusUserMarker(true);
                startLocation();
            }
        };

        View.OnClickListener buttonListenerLocation2 = view -> {
//            //markerWithOrientation = false;
//            floorSelectorView.setFocusUserMarker(true);
//            floorSelectorView.updatePositioningFloor(SECOND_FLOOR_ID);
            onPostFloorChanged();
        };

        startNavBtn.setOnClickListener(buttonListenerLocation);
        button2.setOnClickListener(buttonListenerLocation2);
    }

    private void setRoute(Point from, Point to){
        DirectionsRequest directionsRequest = new DirectionsRequest.Builder()
                .from(from, null)
                .to(to)
                .build();

        SitumSdk.directionsManager().requestDirections(directionsRequest, new Handler<Route>() {
            @Override
            public void onSuccess(Route route) {
                Log.d(TAG, "should not change yet: " + recalculatingRoute);
                removeRoute();
                drawRoute(route);
            }

            @Override
            public void onFailure(Error error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void drawRoute(Route route){
        if (navStarted && recalculatedF1 != null && lastPositioningFloorId != null){
            if (lastPositioningFloorId.equals(currentSelectedFloorID)){
                route = recalculatedF1;
            }
        }else if (navStarted && recalculatedF2 != null && lastPositioningFloorId != null){
            if (lastPositioningFloorId.equals(currentSelectedFloorID)){
                route = recalculatedF2;
            }
        }

        for (RouteSegment segment : route.getSegments()) {
            //draw polyline for each segment
            List<LatLng> latLngs = new ArrayList<>();
            for (Point point : segment.getPoints()) {
                latLngs.add(new LatLng(point.getCoordinate().getLatitude(), point.getCoordinate().getLongitude()));
            }

            PolylineOptions polyLineOptions = new PolylineOptions()
                    .color(Color.GREEN)
                    .width(4f)
                    .zIndex(3)
                    .addAll(latLngs);
            Polyline polyline = map.addPolyline(polyLineOptions);

            if (route.getSegments().get(0).getFloorIdentifier().equals(FIRST_FLOOR_ID)){
                if (recalculatingRoute){
                    outRouteCounter = 0;
                    recalculatingRoute = false;
                    pauseNav = false;

                    routeF1.add(polyline);
                    recalculatedF1 = route;
                }else{
                    routeF1.add(polyline);
                    lastF1Route = route;
                }
            }else if (route.getSegments().get(0).getFloorIdentifier().equals(SECOND_FLOOR_ID)){
                if (recalculatingRoute){
                    outRouteCounter = 0;
                    recalculatingRoute = false;
                    pauseNav = false;

                    routeF2.add(polyline);
                    recalculatedF2 = route;
                }else{
                    routeF2.add(polyline);
                    lastF2Route = route;
                }
            }

            if (waitPostFloorChg && floorChanged){
                Log.d(TAG, "map has been drawn");
                pauseNav = false;
                waitPostFloorChg = false;
            }
        }
    }

    private void removeRoute(){
        for (Polyline polyline : routeF1) {
            polyline.remove();
        }
        routeF1.clear();

        for (Polyline polyline : routeF2) {
            polyline.remove();
        }
        routeF2.clear();
    }

    private void removeMarker(){
        firstPreview = true;

        if (destinationMarker != null){
            destinationMarker.remove();
            destinationMarker = null;
        }

        if (stairMarker != null){
            stairMarker.remove();
            stairMarker = null;
        }

        if (initialLocMarker != null){
            initialLocMarker.remove();
            initialLocMarker = null;
        }
    }

    public static void floorChanged(){
        new Navigation().setDestination();
    }

    private void setDestination(){
        if (!firstPreview){
            plotMarkers();
        }
    }

    private void plotMarkers(){
        Point initialPost = null, from, to;
        Log.d(TAG, "plotting");

        //set the initial position of user
        if (currentPoiPost == 1){
            initialPost = firstLocation.getPosition();
        }else{
            for (Poi x : buildingPoi){
                if (x.getName().equals(selectedPoiNames.get(currentPoiPost - 2))){
                    initialPost = x.getPosition();
                }
            }
        }

        //set from and to to initial position and final destination respectively
        from = initialPost;
        if (destinationPoi == null){
            to = firstLocation.getPosition();
        }else{
            to = destinationPoi.getPosition();
        }

        if (firstPreview){
            Log.d(TAG, "entered");
            if (destinationMarker == null || stairMarker == null || initialLocMarker == null){
                initializePreviewMarkers(); //initialize destination and stair marker
            }

            lastSelectedFloorID = currentSelectedFloorID;

            //plot user current location
            if (currentSelectedFloorID.equals(initialPost.getFloorIdentifier())){
                LatLng startLatLng = new LatLng(initialPost.getCoordinate().getLatitude(),
                        initialPost.getCoordinate().getLongitude());

                initialLocMarker.setPosition(startLatLng);
                initialLocMarker.setVisible(true);
            }

            if (currentSelectedFloorID.equals(to.getFloorIdentifier())){
                if (floorSpan == 2
                        && !initialPost.getFloorIdentifier().equals(to.getFloorIdentifier())){
                    for (Poi poi : buildingPoi){
                        if (poi.getName().equals("Stair")
                                && poi.getFloorIdentifier().equals(currentSelectedFloorID)){
                            LatLng stairLatLng = new LatLng(poi.getCoordinate().getLatitude(),
                                    poi.getCoordinate().getLongitude());

                            stairMarker.setPosition(stairLatLng);
                            stairMarker.setVisible(true);
                            from = poi.getPosition(); //set from position to position of stair
                            break;
                        }
                    }
                }else{
                    stairMarker.setVisible(false);
                }

                LatLng destinationLatLng = new LatLng(to.getCoordinate().getLatitude(),
                        to.getCoordinate().getLongitude());

                destinationMarker.setPosition(destinationLatLng);
                destinationMarker.setVisible(true);
                setRoute(from, to);
            }else{
                if (floorSpan == 2){
                    for (Poi poi : buildingPoi){
                        Log.d(TAG, "plotMarkers: " + buildingPoi.get(5).getFloorIdentifier());
                        if (poi.getName().equals("Stair")
                                && poi.getFloorIdentifier().equals(initialPost.getFloorIdentifier())){
                            LatLng stairLatLng = new LatLng(poi.getCoordinate().getLatitude(),
                                    poi.getCoordinate().getLongitude());

                            stairMarker.setPosition(stairLatLng);
                            stairMarker.setVisible(true);
                            to = poi.getPosition();
                            setRoute(from, to);
                            break;
                        }
                    }
                }
            }

            firstPreview = false;
        }else{
            Log.d(TAG, "entered 2");
            if (lastSelectedFloorID.equals(floorSelectorView.getSelectedFloorId())){
                return;
            }

            lastSelectedFloorID = floorSelectorView.getSelectedFloorId();
            currentSelectedFloorID = floorSelectorView.getSelectedFloorId();

            //plot user current location
            if (currentSelectedFloorID.equals(initialPost.getFloorIdentifier()) && !navStarted){
                LatLng startLatLng = new LatLng(initialPost.getCoordinate().getLatitude(),
                        initialPost.getCoordinate().getLongitude());

                initialLocMarker.setPosition(startLatLng);
                initialLocMarker.setVisible(true);
            }else{
                initialLocMarker.setVisible(false);
            }

            if (currentSelectedFloorID.equals(to.getFloorIdentifier())){
                if (floorSpan == 2
                        && !initialPost.getFloorIdentifier().equals(to.getFloorIdentifier())){
                    for (Poi poi : buildingPoi){
                        if (poi.getName().equals("Stair")
                                && poi.getFloorIdentifier().equals(currentSelectedFloorID)){
                            LatLng stairLatLng = new LatLng(poi.getCoordinate().getLatitude(),
                                    poi.getCoordinate().getLongitude());

                            stairMarker.setPosition(stairLatLng);
                            stairMarker.setVisible(true);
                            from = poi.getPosition();
                            break;
                        }
                    }
                }else{
                    stairMarker.setVisible(false);
                }

                LatLng destinationLatLng = new LatLng(to.getCoordinate().getLatitude(),
                        to.getCoordinate().getLongitude());

                destinationMarker.setPosition(destinationLatLng);
                destinationMarker.setVisible(true);
                setRoute(from, to);

            }else if (floorSpan == 2){
                for (Poi poi : buildingPoi){
                    if (poi.getName().equals("Stair")
                            && poi.getFloorIdentifier().equals(initialPost.getFloorIdentifier())){
                        LatLng stairLatLng = new LatLng(poi.getCoordinate().getLatitude(),
                                poi.getCoordinate().getLongitude());

                        stairMarker.setPosition(stairLatLng);
                        stairMarker.setVisible(true);
                        destinationMarker.setVisible(false);
                        to = poi.getPosition();
                        setRoute(from, to);
                        break;
                    }
                }
            }else{
                initialLocMarker.setVisible(false);
                destinationMarker.setVisible(false);
                stairMarker.setVisible(false);
                removeRoute();
            }
        }
    }

    private void initializePreviewMarkers(){
        Bitmap initialLocScaled;
        Bitmap destinationScaled;
        Bitmap stairScaled;

        Bitmap bitmapInitLoc = BitmapFactory.decodeResource(getResources(), R.drawable.blue_dot);
        initialLocScaled = Bitmap.createScaledBitmap(bitmapInitLoc, bitmapInitLoc.getWidth() / 32,bitmapInitLoc.getHeight() / 32, false);

        if (initialLocMarker == null){
            //add marker to indicate initial Location of user on Google map
            initialLocMarker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(0,0))
                    .zIndex(100) //provide high index so that this marker will be drawn over other markers
                    .flat(true) //set to flat to rotate or tilt with camera
                    .anchor(0.5f,0.5f) //point of image that will be placed at latlong position of marker
                    .visible(false)
                    .icon(BitmapDescriptorFactory.fromBitmap(initialLocScaled)));
        }

        Bitmap bitmapDestination = BitmapFactory.decodeResource(getResources(), R.drawable.destination);
        destinationScaled = Bitmap.createScaledBitmap(bitmapDestination, bitmapDestination.getWidth() / 10,bitmapDestination.getHeight() / 10, false);

        if (destinationMarker == null){
            //add marker to indicate destination on Google map
            destinationMarker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(0,0))
                    .zIndex(90) //provide high index so that this marker will be drawn over other markers
                    .flat(true) //set to flat to rotate or tilt with camera
                    .anchor(0.5f,0.8f) //point of image that will be placed at latlong position of marker
                    .visible(false)
                    .icon(BitmapDescriptorFactory.fromBitmap(destinationScaled)));
        }

        Bitmap bitmapStair = BitmapFactory.decodeResource(getResources(), R.drawable.stair);
        stairScaled = Bitmap.createScaledBitmap(bitmapStair, bitmapStair.getWidth() / 3,bitmapStair.getHeight() / 3, false);

        if (stairMarker == null){
            //add marker to indicate destination on Google map
            stairMarker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(0,0))
                    .zIndex(90) //provide high index so that this marker will be drawn over other markers
                    .flat(true) //set to flat to rotate or tilt with camera
                    .anchor(0.5f,0.5f) //point of image that will be placed at latlong position of marker
                    .visible(false)
                    .icon(BitmapDescriptorFactory.fromBitmap(stairScaled)));
        }
    }

    void startNavigation(){
        SitumSdk.navigationManager().requestNavigationUpdates(navReq, new NavigationListener() {
            @Override
            public void onDestinationReached() {
                mtvDirection.setText("Arrived");
                pauseNav = true;
                toNextDest();
            }

            @Override
            public void onProgress(NavigationProgress navigationProgress) {
                Context context = getApplicationContext();
                mtvDirection.setText(navigationProgress.getCurrentIndication().toText(context));
            }

            @Override
            public void onUserOutsideRoute() {
                mtvDirection.setText("Outside of the route");
                outRouteCounter++;
                if (outRouteCounter >= 5){
                    Log.d(TAG, "change route");
                    mtvDirection.setText("Recalculating route...");
                    pauseNav = true;
                    recalculatingRoute = true;
                    recalculateRoute();
                }
            }
        });
    }

    private void prepareNextPoi(){
        if (currentPoiPost == selectedPoiNames.size()){
            reset(true);

            //alert dialog to inform user of the end of navigation
            AlertDialog.Builder builder = new AlertDialog.Builder(Navigation.this);
            builder.setMessage("Hooray! You have reached the final destination!\nPress 'OK' to return back to home!")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //return back to home page after pressing 'OK'
                            Intent myIntent = new Intent(Navigation.this, Menu.class);
                            Navigation.this.startActivity(myIntent);
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }else{
            currentPoiPost++; //increment current poi position
            Log.d(TAG, "incremented" + currentPoiPost);
            reset(false);

            //alert dialog to inform user that destination has been reached
            AlertDialog.Builder builder = new AlertDialog.Builder(Navigation.this);
            builder.setMessage("Hooray! You have reached your destination!\nPress 'OK' to continue!")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //setup map for next destination
                            setup();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void toNextDest(){
        Point to;
        if (destinationPoi == null){
            to = firstLocation.getPosition();
        }else{
            to = destinationPoi.getPosition();
        }
        if (floorSpan == 1){
            //end navigation if all destinations have been visited
            prepareNextPoi();

        }else if (floorSpan == 2){
            if (lastPositioningFloorId.equals(to.getFloorIdentifier())){
                prepareNextPoi();
            }else{
                Log.d(TAG, "waiting for floor change");
                waitPostFloorChg = true;
                navReq = null;
                navStarted = false;
                SitumSdk.navigationManager().removeUpdates();

                AlertDialog.Builder builder = new AlertDialog.Builder(Navigation.this);
                builder.setMessage("You have reached the Stairs!\nPlease navigate to the next floor to continue navigation to final destination!")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mtvDirection.setText("Please navigate to next floor!");
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }

        }
    }

    private void recalculateRoute(){
        Point to;
        navReq = null;
        navStarted = false;
        SitumSdk.navigationManager().removeUpdates();

        if (destinationPoi == null){
            to = firstLocation.getPosition();
        }else{
            to = destinationPoi.getPosition();
        }

        if (floorSpan == 1){
            setRoute(lastLocation.getPosition(), to);
        }else if (floorSpan == 2){
            if (lastLocation.getFloorIdentifier().equals(to.getFloorIdentifier())){
                setRoute(lastLocation.getPosition(), to);
            }else{
                for (Poi x : buildingPoi){
                    if (x.getName().equals("Stair") && x.getFloorIdentifier().equals(lastLocation.getFloorIdentifier())){
                        setRoute(lastLocation.getPosition(), x.getPosition());
                        break;
                    }
                }
            }
        }
    }

    private void onPostFloorChanged(){
        mtvDirection.setText("Floor changed!");
        markerWithOrientation = false;
        floorSelectorView.setFocusUserMarker(true);

        if (lastLocation.getFloorIdentifier().equals(FIRST_FLOOR_ID)){
            lastPositioningFloorId = SECOND_FLOOR_ID;
            floorSelectorView.updatePositioningFloor(SECOND_FLOOR_ID);
        }else{
            lastPositioningFloorId = FIRST_FLOOR_ID;
            floorSelectorView.updatePositioningFloor(FIRST_FLOOR_ID);
        }
    }

    void stopNavigation(){
        navReq = null;
        navStarted = false;
        SitumSdk.navigationManager().removeUpdates();
        navDescLayout.setVisibility(View.GONE);
        mtvDirection.setText("Navigation");
    }

    private void startLocation(){
        if(locationManager.isRunning()){
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (pauseNav){
                    Log.d(TAG, "Nav has been paused");
                    if (waitPostFloorChg){
                        Log.d(TAG, "waiting for floor change!");
                    }
                    if (waitPostFloorChg
                            //&& !location.getFloorIdentifier().equals(lastLocation.getFloorIdentifier())){
                            && !location.getFloorIdentifier().equals(lastLocation.getFloorIdentifier())
                            && !floorChanged){
                        floorChanged = true;
                        Toast.makeText(Navigation.this, "floor changed!", Toast.LENGTH_SHORT).show();
                        onPostFloorChanged();
                    }

                    return;
                }

                current = location; //store current location of user in current variable
                lastLocation = location;

                //get floor user currently located in
                String currentFloorId = location.getFloorIdentifier();
                if(!currentFloorId.equals(lastPositioningFloorId)){
                    lastPositioningFloorId = currentFloorId;
                    floorSelectorView.updatePositioningFloor(currentFloorId);
                }

                displayPositioning(location);

                //create navigation request if navigation has not started
                if (!navStarted){
                    Route route;
                    startNavBtn.setVisibility(View.GONE);
                    initialLocMarker.setVisible(false); //hide marker of current location

                    if (current.getFloorIdentifier().equals(FIRST_FLOOR_ID)){

                        if (recalculatedF1 != null){
                            route = recalculatedF1;
                        }else{
                            route = lastF1Route;
                        }

                        navReq = new NavigationRequest.Builder()
                                .route(route)
                                .distanceToGoalThreshold(150d)
                                .outsideRouteThreshold(10d)
                                .build();

                    }else if (current.getFloorIdentifier().equals(SECOND_FLOOR_ID)){

                        if (recalculatedF2 != null){
                            route = recalculatedF2;
                        }else{
                            route = lastF2Route;
                        }

                        navReq = new NavigationRequest.Builder()
                                .route(route)
                                .distanceToGoalThreshold(150d)
                                .outsideRouteThreshold(10d)
                                .build();
                    }


                    navDescLayout.setVisibility(View.VISIBLE);
                    startNavigation();
                    navStarted = true;
                }else{
                    //update navigation if navigation has started
                    SitumSdk.navigationManager().updateWithLocation(current);
                }

                progressBar.setVisibility(ProgressBar.GONE);
            }

            @Override
            public void onStatusChanged(@NonNull LocationStatus locationStatus) {
                Log.d(TAG, "onStatusChanged(): " + locationStatus);
            }

            @Override
            public void onError(@NonNull Error error) {
                Log.e(TAG, "onError(): " + error.getMessage());
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(Navigation.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }

        };

        LocationRequest locationRequest = new LocationRequest.Builder()
                .buildingIdentifier(buildingId)
                .useDeadReckoning(true)
                .build();

        SitumSdk.locationManager().requestLocationUpdates(locationRequest, locationListener);
    }

    /**
     * Detects if the user is inside the selected floor or not, then show/hides the marker and ground overlay
     *
     * @param location Location
     */
    private void displayPositioning(Location location){
        if(!floorSelectorView.focusUserMarker() &&
                !location.getFloorIdentifier().equals(floorSelectorView.getSelectedFloorId())) {
            positionAnimator.clear();
            if (groundOverlay != null) {
                groundOverlay.remove();
                groundOverlay = null;
            }
            if(marker != null){
                marker.remove();
                marker = null;
            }
        }else{
            //obtain coordinates from location
            LatLng latLng = new LatLng(location.getCoordinate().getLatitude(),
                    location.getCoordinate().getLongitude());
            if (marker == null){
                initializeMarker(latLng);
            }
            if (groundOverlay == null) {
                initializeGroundOverlay();
            }

            markerWithOrientation = false; //set marker orientation to false by default
            updateMarkerIcon();
            positionAnimator.animate(marker, groundOverlay, location);
            centerInUser(location);

        }
    }

    private void initializeMarker(LatLng latLng) {
        //get the bitmap of image for position
        Bitmap bitmapArrow = BitmapFactory.decodeResource(getResources(), R.drawable.blue_dot);
        //scale bitmap of image 32 times smaller than original size
        Bitmap arrowScaled = Bitmap.createScaledBitmap(bitmapArrow, bitmapArrow.getWidth() / 32,bitmapArrow.getHeight() / 32, false);

        //add marker to indicate location of user on Google map
        marker = map.addMarker(new MarkerOptions()
                .position(latLng)
                .zIndex(100) //provide high index so that this marker will be drawn over other markers
                .flat(true) //set to flat to rotate or tilt with camera
                .anchor(0.5f,0.5f) //point of image that will be placed at latlong position of marker
                .icon(BitmapDescriptorFactory.fromBitmap(arrowScaled)));
    }

    private void initializeGroundOverlay() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1; //maintain original size when decoding image
        Bitmap bitmapPosbg = BitmapFactory.decodeResource(getResources(), R.drawable.situm_posbg, opts);
        GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bitmapPosbg))
                .anchor(0.5f, 0.5f)
                .position(new LatLng(0, 0), 2) //make ground overlay 2m wide
                .zIndex(2); //provide low index so that this marker will be drawn below other markers
        groundOverlay = map.addGroundOverlay(groundOverlayOptions);
    }

    private void centerInUser(Location location) {
        float bearing = (location.hasBearing()) && location.isIndoor() ? (float) (location.getBearing().degrees()) : map.getCameraPosition().bearing;

        LatLng latLng = new LatLng(location.getCoordinate().getLatitude(), location.getCoordinate().getLongitude());

        //Skip if no change in location and change in bearing
        boolean skipAnimation = lastCameraLatLng != null && lastCameraLatLng.equals(latLng)
                && (Math.abs(bearing - lastCameraBearing)) < MIN_CHANGE_IN_BEARING_TO_ANIMATE_CAMERA;
        lastCameraLatLng = latLng;
        lastCameraBearing = bearing;
        if (!skipAnimation) {
            CameraPosition cameraPosition = new CameraPosition.Builder(map.getCameraPosition())
                    .target(latLng)
                    .bearing(bearing)
                    .build();

            //animate camera with duration of 0.6 seconds
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), UPDATE_LOCATION_ANIMATION_TIME, null);
        }
    }

    private void updateMarkerIcon() {
        boolean newLocationHasOrientation = (current.hasBearing()) && current.isIndoor();
        if (markerWithOrientation == newLocationHasOrientation) {
            return;
        }
        markerWithOrientation = newLocationHasOrientation;

        BitmapDescriptor bitmapDescriptor; //used to set the image of the marker icon
        Bitmap bitmapScaled;
        if(markerWithOrientation){
            Bitmap bitmapArrow = BitmapFactory.decodeResource(getResources(), R.drawable.pose);
            bitmapScaled = Bitmap.createScaledBitmap(bitmapArrow, bitmapArrow.getWidth() / 4,bitmapArrow.getHeight() / 4, false);
        } else {
            Bitmap bitmapCircle = BitmapFactory.decodeResource(getResources(), R.drawable.blue_dot);
            bitmapScaled = Bitmap.createScaledBitmap(bitmapCircle, bitmapCircle.getWidth() / 32,bitmapCircle.getHeight() / 32, false);
        }
        bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmapScaled);
        marker.setIcon(bitmapDescriptor);
    }

    private void stopLocation(){
        stopNavigation();

        if (!locationManager.isRunning()){
            return;
        }

        locationManager.removeUpdates(locationListener);
        SitumSdk.locationManager().removeUpdates(locationListener);
        current = null;
        positionAnimator.clear();

        if (groundOverlay != null) {
            groundOverlay.remove();
            groundOverlay = null;
        }
        if(marker != null){
            marker.remove();
            marker = null;
        }
    }

    /**
     * Getting permissions required for localization
     *
     */
    private void requestPermissions(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(Navigation.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    LOCATION_BLUETOOTH_REQUEST_CODE);
        }else {
            ActivityCompat.requestPermissions(Navigation.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_REQUEST_CODE);
        }

    }

    /**
     * Checking if required permission is granted
     *
     */
    private void checkPermissions() {
        boolean hasFineLocationPermission = ContextCompat.checkSelfPermission(Navigation.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            boolean hasBluetoothScanPermission = ContextCompat.checkSelfPermission(Navigation.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean hasBluetoothConnectPermission = ContextCompat.checkSelfPermission(Navigation.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

            if(!hasBluetoothConnectPermission || !hasBluetoothScanPermission || !hasFineLocationPermission){
                if (ActivityCompat.shouldShowRequestPermissionRationale(Navigation.this, Manifest.permission.BLUETOOTH_SCAN)
                        || ActivityCompat.shouldShowRequestPermissionRationale(Navigation.this, Manifest.permission.BLUETOOTH_CONNECT)
                        || ActivityCompat.shouldShowRequestPermissionRationale(Navigation.this, Manifest.permission.ACCESS_FINE_LOCATION)) {

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
                if (ActivityCompat.shouldShowRequestPermissionRationale(Navigation.this, Manifest.permission.ACCESS_FINE_LOCATION)) {

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

        if (requestCode == ACCESS_FINE_LOCATION_REQUEST_CODE) {
            if (!(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finishActivity(requestCode);
            }
        }else if(requestCode == LOCATION_BLUETOOTH_REQUEST_CODE){
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

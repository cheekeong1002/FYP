package com.example.fypmock;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.situm.sdk.SitumSdk;
import es.situm.sdk.directions.DirectionsRequest;
import es.situm.sdk.error.Error;
import es.situm.sdk.model.cartography.Poi;
import es.situm.sdk.model.cartography.Point;
import es.situm.sdk.model.directions.Route;
import es.situm.sdk.model.location.Location;
import es.situm.sdk.utils.Handler;

public class DisplayPoiOrder extends GetBuildingID {

    private String buildingID;
    private final GetPoiUseCase getPoiUseCase = new GetPoiUseCase();
    private final List<Poi> buildingPoi = new ArrayList<>();

    private String navMethod;
    ArrayList<String> selectedPoiList = new ArrayList<>();
    private final ArrayList<String[]> allCombinations = new ArrayList<>();
    private ArrayList<String[]> pairPoi = new ArrayList<>();
    private ArrayList<Double> pairPoiDistance = new ArrayList<>();
    private boolean calNextFloor = false;
    private boolean nextFloorCalculated = false;
    private String first = "a";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_poi_order);

        navMethod = (String) getIntent().getStringExtra("SELECTED_METHOD");
        selectedPoiList.addAll(getIntent().getParcelableArrayListExtra("SELECTED_POIS"));
        Toast.makeText(this, selectedPoiList.toString(), Toast.LENGTH_SHORT).show();

        Toast.makeText(this, navMethod, Toast.LENGTH_SHORT).show();

        buildingID = getBuildingID();

        //Initialize SitumSdk
        SitumSdk.init(this);

        // Set credentials
        SitumSdk.configuration().setUserPass("p19011503@student.newinti.edu.my", "T@nck2001");
        SitumSdk.configuration().setApiKey("p19011503@student.newinti.edu.my", "791bb3e3a8856145aed74aae9e138e8c1d45289fe7584b63c60ae60802c426c1");

        getAllCombinations(selectedPoiList, 0);
        setPoiOfBuilding();
    }

    private void getShortestPath(){
        String fromName = "", toName = "";
        int combCounter = 0;
        double shortestDistance = 0;
        ArrayList<String> shortestPath = new ArrayList<>();

        for (String[] combination : allCombinations){
            double totalDistance = 0;
            Log.d("TAG", "combination: " + Arrays.toString(combination));

            for (int i=0; i<combination.length; i++){
                if (i==0){
                    //start location
                    fromName = "Age's Ago";
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
            Log.d("TAG", "getShortestPath: " + totalDistance);

            if (shortestDistance == 0){
                shortestDistance = totalDistance;
                //add start location
                shortestPath.clear();
                shortestPath.add("Age's Ago");
                shortestPath.addAll(Arrays.asList(allCombinations.get(combCounter)));
            }else{
                if (totalDistance < shortestDistance){
                    shortestDistance = totalDistance;
                    //add start location
                    shortestPath.clear();
                    shortestPath.add("Age's Ago");
                    shortestPath.addAll(Arrays.asList(allCombinations.get(combCounter)));
                }
            }

            combCounter++;
        }

        Log.d("TAG", "shortest distance: " + shortestDistance);
        Log.d("TAG", "shortest path: " + shortestPath);
    }

    private void getPairDistance(int cid){
        String fromName = "", toName = "";
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

        if (!from.getFloorIdentifier().equals(to.getFloorIdentifier())){
            if (calNextFloor){
                nextFloorCalculated = true;

                for(Poi poi : buildingPoi){
                    if (poi.getName().equals("Stair") && poi.getFloorIdentifier().equals(to.getFloorIdentifier())){
                        from = poi.getPosition();
                        Log.d("TAG", "from1: " + poi);
                    }
                }
            }else{
                calNextFloor = true;

                for(Poi poi : buildingPoi){
                    if (poi.getName().equals("Stair") && poi.getFloorIdentifier().equals(from.getFloorIdentifier())){
                        to = poi.getPosition();
                        Log.d("TAG", "to1: " + poi);
                        Log.d("TAG", "should stop: " + cid);
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

                //get from position
                if (x == 0) {
                    //start location
                    fromName = buildingPoi.get(0).getName();
                    Log.d("TAG", "from: " + buildingPoi.get(0).getName());
                } else {
                    for (Poi poi : buildingPoi) {
                        if (poi.getName().equals(combination[x - 1])) {
                            fromName = poi.getName();
                            Log.d("TAG", "from: " + poi.getName());
                        }
                    }
                }

                //get to position
                for (Poi poi : buildingPoi) {
                    if (poi.getName().equals(combination[x])) {
                        toName = poi.getName();
                        Log.d("TAG", "to: " + poi.getName());
                    }
                }

                boolean pairExists = checkPairExist(fromName, toName);

                if (!pairExists) {
                    String[] temp = {fromName, toName};
                    pairPoi.add(temp);
                    Log.d("TAG", "printer: ");
                }
            }
            currentIteration++;
        }

        getPairDistance(0);
    }

    private void setPoiOfBuilding(){
        getPoiUseCase.get(buildingID, new GetPoiUseCase.Callback() {
            @Override
            public void onSuccess(List<Poi> pois) {
                buildingPoi.addAll(pois);
                formPoiPairs();
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
            String[] temp = selectedPoiList.toArray(new String[0]);
            allCombinations.add(temp);
            return;
        }

        for (int i=cid; i<selectedPoiList.size(); i++){
            swapPosition(selectedPoiList, i, cid);
            getAllCombinations(selectedPoiList, cid+1);
            swapPosition(selectedPoiList, i, cid);
        }
    }
}
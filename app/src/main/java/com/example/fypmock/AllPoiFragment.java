package com.example.fypmock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.protobuf.Value;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AllPoiFragment extends Fragment {

    FirebaseDatabase mDatabase;
    FirebaseAuth mAuth;

    private AllPoiFragment.AllPoiAdapter adapter;
    private ArrayList<String> orderDisplayPOI = new ArrayList<>();
    private ArrayList<String> selectedPOIs = new ArrayList<>();
    private ArrayList<String> data = new ArrayList<>();
    private ArrayList<String> favPoiList = new ArrayList<>();
    private ArrayList<String> allPoiName = new ArrayList<>();
    private ListView mlv_fav;
    private SearchView msv_searchView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_all_poi, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        FirebaseDatabase.getInstance().getReference("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("favPoiNames")
                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()){
                    if (!task.getResult().getValue().equals("")){
                        String [] tempFavList = task.getResult().getValue().toString().split(", ");
                        Arrays.sort(tempFavList); //re-arrange list based on alphabet

                        if (!Arrays.asList(tempFavList).equals(favPoiList)){
                            favPoiList.clear();
                            favPoiList.addAll(Arrays.asList(tempFavList));
                            adapter.notifyDataSetChanged();
                        }
                    }else{
                        if (favPoiList.size() != 0){
                            favPoiList.clear();
                            adapter.notifyDataSetChanged();
                        }
                    }
                }else{
                    Toast.makeText(getActivity(), "Failed to retrieve favourite POI names!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mlv_fav = view.findViewById(R.id.lv_fav);
        msv_searchView = view.findViewById(R.id.searchView);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        SharedPreferences prefs = getActivity().getSharedPreferences("SelectedPOIs", getActivity().MODE_PRIVATE);
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = new JSONArray(prefs.getString("SelectedPOIsKey", ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("TAG", "onViewCreated: " + jsonArray);
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                selectedPOIs.add(jsonArray.get(i).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Button cancel_btn = view.findViewById(R.id.cancel_button);
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dataToPass = new Intent();
                dataToPass.putExtra("selectedPoi", "");
                // Activity finished ok, return the data
                getActivity().setResult(Activity.RESULT_OK, dataToPass);
                Toast.makeText(getActivity(), "Action Canceled!", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        });

        getAllPoi();
    }

    private void getAllPoi(){
        FirebaseDatabase.getInstance().getReference("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("favPoiNames")
                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()){
                    String [] tempFavList = task.getResult().getValue().toString().split(", ");
                    Arrays.sort(tempFavList); //re-arrange list based on alphabet
                    favPoiList.addAll(Arrays.asList(tempFavList));

                    FirebaseDatabase.getInstance().getReference("History")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (task.isSuccessful()){
                                int counter = 0;
                                ArrayList<String> lastVisitedPOI = new ArrayList<>();
                                ArrayList<String> suggestedPOI = new ArrayList<>();

                                for (DataSnapshot data: task.getResult().getChildren()){
                                    lastVisitedPOI.add(data.getValue().toString());
                                }

                                Collections.reverse(lastVisitedPOI);
                                for (String visitedPOI: lastVisitedPOI){
                                    if (counter >= 5){
                                        break;
                                    }

                                    if (!suggestedPOI.contains(visitedPOI)){
                                        suggestedPOI.add(visitedPOI);
                                        counter++;
                                    }
                                }

                                String[] tempAllPoi = getResources().getStringArray(R.array.POIs);
                                Arrays.sort(tempAllPoi);
                                data.addAll(Arrays.asList(tempAllPoi));
                                allPoiName.addAll(Arrays.asList(tempAllPoi));
                                Collections.reverse(suggestedPOI);

                                for (String poiName: suggestedPOI){
                                    data.remove(poiName);
                                    allPoiName.remove(poiName);
                                    data.add(0, poiName);
                                    allPoiName.add(0, poiName);
                                }
                                orderDisplayPOI.addAll(allPoiName);

                                adapter = new AllPoiFragment.AllPoiAdapter(getActivity(), R.layout.item, data);
                                mlv_fav.setAdapter(adapter);
                                mlv_fav.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        for (String poiName : selectedPOIs){
                                            if (poiName.equals(data.get(position))){
                                                Toast.makeText(getActivity(), poiName + " has already been selected!", Toast.LENGTH_LONG).show();
                                                return;
                                            }
                                        }

                                        Intent dataToPass = new Intent();
                                        dataToPass.putExtra("selectedPoi", data.get(position));
                                        // Activity finished ok, return the data
                                        getActivity().setResult(Activity.RESULT_OK, dataToPass);
                                        getActivity().finish();
                                    }
                                });

                                msv_searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                                    @Override
                                    public boolean onQueryTextSubmit(String query) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onQueryTextChange(String newText) {
                                        if (newText.equals("")){
                                            data.clear();
                                            data.addAll(orderDisplayPOI);
                                        }else{
                                            ArrayList<String> filteredData = new ArrayList<>();

                                            for (String poiName : allPoiName){
                                                if (poiName.toLowerCase().contains(newText.toLowerCase())){
                                                    filteredData.add(poiName);
                                                }
                                            }

                                            Collections.sort(filteredData);
                                            data.clear();
                                            data.addAll(filteredData);
                                        }

                                        adapter.notifyDataSetChanged();

                                        return false;
                                    }
                                });
                            }else{
                                Toast.makeText(getActivity(), "Failed to retrieve visited POI history!!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    Toast.makeText(getActivity(), "Failed to retrieve favourite POI names!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class AllPoiAdapter extends ArrayAdapter<String> {
        private int layout;

        public AllPoiAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
            layout = resource;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            AllPoiFragment.ViewHolder mainViewholder = null;

            if (convertView == null){
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                AllPoiFragment.ViewHolder viewHolder = new AllPoiFragment.ViewHolder();
                viewHolder.poiName = (TextView) convertView.findViewById(R.id.tv_poiName);
                viewHolder.fav = (ImageView) convertView.findViewById(R.id.fav);
                viewHolder.unfav = (ImageView) convertView.findViewById(R.id.unfav);

                convertView.setTag(viewHolder);
                mainViewholder = (AllPoiFragment.ViewHolder) viewHolder;
            }else
                mainViewholder = (AllPoiFragment.ViewHolder) convertView.getTag();

            mainViewholder.poiName.setText(getItem(position));
            AllPoiFragment.ViewHolder finalMainViewholder = mainViewholder;

            mainViewholder.fav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDatabase.getReference("Users")
                            .child(mAuth.getCurrentUser().getUid()).child("favPoiNames")
                            .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (task.isSuccessful()){
                                if (!task.getResult().getValue().equals("")){
                                    String[] favPoiNames = task.getResult().getValue().toString().split(", ");
                                    favPoiList.clear();
                                    favPoiList.addAll(Arrays.asList(favPoiNames));
                                }else{
                                    favPoiList.clear();
                                }

                                StringBuilder temp = new StringBuilder();
                                favPoiList.add(data.get(position));
                                Collections.sort(favPoiList);

                                int loopCounter = 0;
                                for (String favPoiName : favPoiList){
                                    if (!favPoiName.equals(data.get(position))){
                                        temp.append(favPoiName);
                                        if (loopCounter == favPoiList.size() - 1){
                                            break;
                                        }else{
                                            temp.append(", ");
                                        }
                                    }
                                }

                                //delete ", " if ends with it
                                if (temp.length() >= 2){
                                    if (temp.charAt(temp.length() - 2) == ',' && temp.charAt(temp.length()-1) == ' '){
                                        temp.delete(temp.length() - 2, temp.length());
                                    }
                                }

                                mDatabase.getReference("Users")
                                        .child(mAuth.getCurrentUser().getUid()).child("favPoiNames")
                                        .setValue(temp.toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            String poiName = data.get(position);
                                            finalMainViewholder.unfav.setVisibility(View.VISIBLE);
                                            finalMainViewholder.fav.setVisibility(View.GONE);
                                            Toast.makeText(getActivity(), poiName + " removed from favourites!", Toast.LENGTH_SHORT).show();

                                        }else{
                                            Toast.makeText(getActivity(), "Failed to update favourite POIs to database!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }else{
                                Toast.makeText(getActivity(), "Failed to receive favourite POIs from database!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

            mainViewholder.unfav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDatabase.getReference("Users")
                            .child(mAuth.getCurrentUser().getUid()).child("favPoiNames")
                            .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (task.isSuccessful()){
                                if (!task.getResult().getValue().equals("")){
                                    String[] favPoiNames = task.getResult().getValue().toString().split(", ");
                                    favPoiList.clear();
                                    favPoiList.addAll(Arrays.asList(favPoiNames));
                                }else{
                                    favPoiList.clear();
                                }

                                StringBuilder temp = new StringBuilder();
                                favPoiList.add(data.get(position));
                                Collections.sort(favPoiList);

                                for (String favPoiName : favPoiList){
                                    temp.append(favPoiName);
                                    if (favPoiName.equals(favPoiList.get(favPoiList.size() - 1))){
                                        break;
                                    }else{
                                        temp.append(", ");
                                    }
                                }

                                mDatabase.getReference("Users")
                                        .child(mAuth.getCurrentUser().getUid()).child("favPoiNames")
                                        .setValue(temp.toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            String poiName = data.get(position);
                                            finalMainViewholder.fav.setVisibility(View.VISIBLE);
                                            finalMainViewholder.unfav.setVisibility(View.GONE);
                                            Toast.makeText(getActivity(), poiName + " added to favourites!", Toast.LENGTH_SHORT).show();

                                        }else{
                                            Toast.makeText(getActivity(), "Failed to update favourite POIs to database!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }else{
                                Toast.makeText(getActivity(), "Failed to receive favourite POIs from database!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

            for (String selectedPoiName : selectedPOIs){
                if (getItem(position).equals(selectedPoiName)){
                    mainViewholder.poiName.setTextColor(Color.parseColor("#808080"));
                    break;
                }
                mainViewholder.poiName.setTextColor(Color.parseColor("#000000"));
            }

            if (favPoiList.size() == 0){
                mainViewholder.fav.setVisibility(View.GONE);
                mainViewholder.unfav.setVisibility(View.VISIBLE);
            }else{
                for (String favPoiName : favPoiList){
                    if (getItem(position).equals(favPoiName)){
                        mainViewholder.fav.setVisibility(View.VISIBLE);
                        mainViewholder.unfav.setVisibility(View.GONE);
                        break;
                    }
                    mainViewholder.fav.setVisibility(View.GONE);
                    mainViewholder.unfav.setVisibility(View.VISIBLE);
                }
            }

            return convertView;
        }
    }

    public class ViewHolder {
        TextView poiName;
        ImageView fav, unfav;
    }
}
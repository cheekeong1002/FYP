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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FavouriteFragment extends Fragment {

    FirebaseDatabase mDatabase;
    FirebaseAuth mAuth;

    private FavPoiListAdapter adapter;
    private ArrayList<String> selectedPOIs = new ArrayList<>();
    private ArrayList<String> data = new ArrayList<>();
    private ArrayList<String> favPoiList = new ArrayList<>();
    private ListView mlv_fav;

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

                        if (!Arrays.asList(tempFavList).equals(data)){
                            data.clear();
                            data.addAll(Arrays.asList(tempFavList));
                            adapter.notifyDataSetChanged();
                        }
                    }else{
                        if (data.size() != 0){
                            data.clear();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favourite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mlv_fav = view.findViewById(R.id.lv_fav);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        SharedPreferences prefs = getActivity().getSharedPreferences("SelectedPOIs", getActivity().MODE_PRIVATE);
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(prefs.getString("SelectedPOIsKey", null));
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

        getFavPoi();
    }

    private void getFavPoi(){
        FirebaseDatabase.getInstance().getReference("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("favPoiNames")
                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()){
                    if (task.getResult().getValue().toString().equals("")){
                        Toast.makeText(getActivity(), "No Favourite POI!", Toast.LENGTH_SHORT).show();
                    }else{
                        String [] tempList = task.getResult().getValue().toString().split(", ");
                        Arrays.sort(tempList); //re-arrange list based on alphabet
                        data.addAll(Arrays.asList(tempList));
                    }

                    adapter = new FavPoiListAdapter(getActivity(), R.layout.item, data);
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
                }else{
                    Toast.makeText(getActivity(), "Failed to retrieve favourite POI names!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class FavPoiListAdapter extends ArrayAdapter<String> {
        private int layout;

        public FavPoiListAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
            layout = resource;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder mainViewholder = null;

            if (convertView == null){
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                FavouriteFragment.ViewHolder viewHolder = new ViewHolder();
                viewHolder.poiName = (TextView) convertView.findViewById(R.id.tv_poiName);
                viewHolder.fav = (ImageView) convertView.findViewById(R.id.fav);
                viewHolder.unfav = (ImageView) convertView.findViewById(R.id.unfav);

                convertView.setTag(viewHolder);
                mainViewholder = (ViewHolder) viewHolder;
            }else
                mainViewholder = (ViewHolder) convertView.getTag();

            mainViewholder.poiName.setText(getItem(position));
            ViewHolder finalMainViewholder = mainViewholder;

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
                                }

                                StringBuilder temp = new StringBuilder();
                                favPoiList.add(data.get(position));
                                Collections.sort(favPoiList);

                                for (String favPoiName : favPoiList){
                                    if (!favPoiName.equals(data.get(position))){
                                        temp.append(favPoiName);
                                        if (favPoiName.equals(favPoiList.get(favPoiList.size() - 1))){
                                            break;
                                        }else{
                                            temp.append(", ");
                                        }
                                    }
                                }

                                mDatabase.getReference("Users")
                                        .child(mAuth.getCurrentUser().getUid()).child("favPoiNames")
                                        .setValue(temp.toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            String poiName = data.get(position);
                                            data.remove(position);
                                            notifyDataSetChanged();
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

            for(String selectedPoiName : selectedPOIs){
                if (getItem(position).equals(selectedPoiName)){
                    mainViewholder.poiName.setTextColor(Color.parseColor("#808080"));
                    break;
                }
                mainViewholder.poiName.setTextColor(Color.parseColor("#000000"));
            }

            for (String favPoiName : data){
                if (getItem(position).equals(favPoiName)){
                    mainViewholder.fav.setVisibility(View.VISIBLE);
                    mainViewholder.unfav.setVisibility(View.GONE);
                    break;
                }
                mainViewholder.fav.setVisibility(View.GONE);
                mainViewholder.unfav.setVisibility(View.VISIBLE);
            }

            return convertView;
        }
    }

    public class ViewHolder {
        TextView poiName;
        ImageView fav, unfav;
    }
}
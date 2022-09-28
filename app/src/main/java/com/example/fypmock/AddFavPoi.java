package com.example.fypmock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AddFavPoi extends AppCompatActivity {

    FirebaseDatabase mDatabase;
    FirebaseAuth mAuth;

    private AllPoiAdapter adapter;
    private ArrayList<String> selectedPOIs = new ArrayList<>();
    private ArrayList<String> data = new ArrayList<>();
    private ArrayList<String> favPoiList = new ArrayList<>();
    private ArrayList<String> allPoiName = new ArrayList<>();
    private ListView mlv_fav;
    private SearchView msv_searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_fav_poi);

        mlv_fav = findViewById(R.id.lv_fav);
        msv_searchView = findViewById(R.id.searchView);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        Button cancel_btn = findViewById(R.id.cancel_button);
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dataToPass = new Intent();
                dataToPass.putExtra("selectedPoi", "");
                // Activity finished ok, return the data
                setResult(Activity.RESULT_OK, dataToPass);
                Toast.makeText(AddFavPoi.this, "Action Canceled!", Toast.LENGTH_SHORT).show();
                finish();
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

                    String[] tempAllPoi = getResources().getStringArray(R.array.POIs);
                    Arrays.sort(tempAllPoi);
                    data.addAll(Arrays.asList(tempAllPoi));
                    allPoiName.addAll(Arrays.asList(tempAllPoi));

                    adapter = new AllPoiAdapter(AddFavPoi.this, R.layout.item, data);
                    mlv_fav.setAdapter(adapter);
                    mlv_fav.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            for (String poiName : favPoiList){
                                if (poiName.equals(data.get(position))){
                                    Toast.makeText(AddFavPoi.this, poiName + " has already been added to favourites!", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            }

                            Intent dataToPass = new Intent();
                            dataToPass.putExtra("newFavPoi", data.get(position));
                            // Activity finished ok, return the data
                            setResult(Activity.RESULT_OK, dataToPass);
                            finish();
                        }
                    });

                    msv_searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            return false;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            ArrayList<String> filteredData = new ArrayList<>();

                            for (String poiName : allPoiName){
                                if (poiName.toLowerCase().contains(newText.toLowerCase())){
                                    filteredData.add(poiName);
                                }
                            }

                            if (filteredData.size() > 0){
                                Collections.sort(filteredData);
                                data.clear();
                                data.addAll(filteredData);
                                adapter.notifyDataSetChanged();
                            }else{
                                data.clear();
                                adapter.notifyDataSetChanged();
                            }

                            return false;
                        }
                    });
                }else{
                    Toast.makeText(AddFavPoi.this, "Failed to retrieve favourite POI names!", Toast.LENGTH_SHORT).show();
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
            ViewHolder mainViewholder = null;

            if (convertView == null){
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.poiName = (TextView) convertView.findViewById(R.id.tv_poiName);
                viewHolder.fav = (ImageView) convertView.findViewById(R.id.fav);
                viewHolder.unfav = (ImageView) convertView.findViewById(R.id.unfav);

                convertView.setTag(viewHolder);
                mainViewholder = (ViewHolder) viewHolder;
            }else
                mainViewholder = (ViewHolder) convertView.getTag();

            mainViewholder.poiName.setText(getItem(position));

            for (String favPoiName : favPoiList){
                if (getItem(position).equals(favPoiName)){
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
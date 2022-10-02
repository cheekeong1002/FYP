package com.example.fypmock;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FavouritePoiActivity extends AppCompatActivity {

    FirebaseDatabase mDatabase;
    FirebaseAuth mAuth;

    private FavPoiListAdapter adapter;
    private ArrayList<String> data = new ArrayList<>();
    private ArrayList<String> favPoiList = new ArrayList<>();
    private ListView mlv_fav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite_poi);

        mlv_fav = findViewById(R.id.lv_fav);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        Button add_btn = findViewById(R.id.btn_add);
        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FavouritePoiActivity.this, AddFavPoi.class);
                resultLauncher.launch(intent);
            }
        });

        getFavPoi();
    }

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        if (data.getStringExtra("newFavPoi") == null){
                            return;
                        }

                        addToArray(data.getStringExtra("newFavPoi"));
                    }
                }
            });

    private void addToArray(String newFavPoiName){
        mDatabase.getReference("Users")
                .child(mAuth.getCurrentUser().getUid()).child("favPoiNames")
                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.getResult().getValue().equals("")){
                    String[] favPoiNames = task.getResult().getValue().toString().split(", ");
                    favPoiList.clear();
                    favPoiList.addAll(Arrays.asList(favPoiNames));
                }else{
                    favPoiList.clear();
                }

                if (!newFavPoiName.equals("")){
                    StringBuilder temp = new StringBuilder();
                    favPoiList.add(newFavPoiName);
                    Collections.sort(favPoiList);

                    for (String favPoiName : favPoiList){
                        temp.append(favPoiName);
                        if (favPoiName.equals(favPoiList.get(favPoiList.size() - 1))){
                            break;
                        }else{
                            temp.append(", ");
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
                                data.add(newFavPoiName);
                                Collections.sort(data);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(FavouritePoiActivity.this, newFavPoiName + " has been added to favourite POIs!", Toast.LENGTH_LONG).show();

                            }else{
                                Toast.makeText(FavouritePoiActivity.this, "Failed to update favourite POIs to database!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void getFavPoi(){
        FirebaseDatabase.getInstance().getReference("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("favPoiNames")
                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()){
                    if (task.getResult().getValue().toString().equals("")){
                        Toast.makeText(FavouritePoiActivity.this, "No Favourite POI!", Toast.LENGTH_SHORT).show();
                    }else{
                        String [] tempList = task.getResult().getValue().toString().split(", ");
                        Arrays.sort(tempList); //re-arrange list based on alphabet
                        data.addAll(Arrays.asList(tempList));
                    }

                    adapter = new FavPoiListAdapter(FavouritePoiActivity.this, R.layout.item, data);
                    mlv_fav.setAdapter(adapter);
                }else{
                    Toast.makeText(FavouritePoiActivity.this, "Failed to retrieve favourite POI names!", Toast.LENGTH_SHORT).show();
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
                ViewHolder viewHolder = new ViewHolder();
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
                                            data.remove(position);
                                            notifyDataSetChanged();
                                            finalMainViewholder.unfav.setVisibility(View.VISIBLE);
                                            finalMainViewholder.fav.setVisibility(View.GONE);
                                            Toast.makeText(FavouritePoiActivity.this, poiName + " removed from favourites!", Toast.LENGTH_SHORT).show();

                                        }else{
                                            Toast.makeText(FavouritePoiActivity.this, "Failed to update favourite POIs to database!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }else{
                                Toast.makeText(FavouritePoiActivity.this, "Failed to receive favourite POIs from database!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

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
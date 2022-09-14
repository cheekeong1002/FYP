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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SelectPoi extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseDatabase mDatabase;

    private ArrayList<String> data = new ArrayList<String>();
    private ArrayList<String> favPoiList = new ArrayList<String>();
    private ListView lv;
    private Button add_btn, calRoute_btn;
    private SelectedPoiListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        setContentView(R.layout.activity_select_poi);
        lv = findViewById(R.id.lv_selectedPoi);
        add_btn = findViewById(R.id.btn_add);
        calRoute_btn = findViewById(R.id.btn_calRoute);

        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectPoi.this, AddPoi.class);
                resultLauncher.launch(intent);
            }
        });

        calRoute_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectPoi.this, DisplayPoiOrder.class);
                startActivity(intent);
            }
        });

        getSelectedPoi();
    }

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        addToArray(data.getStringExtra("selectedPoi"));
                    }
                }
            });

    private void addToArray(String newPoiName){
        data.add(newPoiName);
        adapter.notifyDataSetChanged();
        Toast.makeText(this, newPoiName + " has been added to selected POIs!", Toast.LENGTH_LONG).show();
    }

    private void getSelectedPoi(){
        mDatabase.getReference("Users")
                .child(mAuth.getCurrentUser().getUid()).child("favPoiNames")
                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()){
                    String [] tempList = task.getResult().getValue().toString().split(", ");
                    Arrays.sort(tempList); //re-arrange list based on alphabet
                    favPoiList.addAll(Arrays.asList(tempList));

                    for (int i=0; i<10; i++){
                        data.add("Shop " + i);
                    }

                    adapter = new SelectedPoiListAdapter(SelectPoi.this, R.layout.item_with_btn, data);
                    lv.setAdapter(adapter);
                }else{
                    Toast.makeText(SelectPoi.this, "Failed to retrieve favourite POI names!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class SelectedPoiListAdapter extends ArrayAdapter<String>{
        private int layout;

        public SelectedPoiListAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
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
                viewHolder.poiName = (TextView) convertView.findViewById(R.id.tv_username);
                viewHolder.delete_btn = (Button) convertView.findViewById(R.id.btn_delete);
                viewHolder.fav = (ImageView) convertView.findViewById(R.id.fav);
                viewHolder.unfav = (ImageView) convertView.findViewById(R.id.unfav);

                viewHolder.delete_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String poiName = data.get(position);
                        data.remove(position);
                        notifyDataSetChanged();
                        Toast.makeText(SelectPoi.this, poiName + " has been removed!", Toast.LENGTH_SHORT).show();
                    }
                });

                viewHolder.fav.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDatabase.getReference("Users")
                                .child(mAuth.getCurrentUser().getUid()).child("favPoiNames")
                                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                if (task.isSuccessful()){
                                    String[] favPoiNames = task.getResult().getValue().toString().split(", ");
                                    favPoiList.clear();
                                    favPoiList.addAll(Arrays.asList(favPoiNames));

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
                                                viewHolder.unfav.setVisibility(View.VISIBLE);
                                                viewHolder.fav.setVisibility(View.GONE);
                                                Toast.makeText(SelectPoi.this, poiName + " removed from favourites!", Toast.LENGTH_SHORT).show();

                                            }else{
                                                Toast.makeText(SelectPoi.this, "Failed to update favourite POIs to database!", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }else{
                                    Toast.makeText(SelectPoi.this, "Failed to receive favourite POIs from database!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });

                viewHolder.unfav.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDatabase.getReference("Users")
                                .child(mAuth.getCurrentUser().getUid()).child("favPoiNames")
                                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                if (task.isSuccessful()){
                                    String[] favPoiNames = task.getResult().getValue().toString().split(", ");
                                    favPoiList.clear();
                                    favPoiList.addAll(Arrays.asList(favPoiNames));

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
                                                viewHolder.fav.setVisibility(View.VISIBLE);
                                                viewHolder.unfav.setVisibility(View.GONE);
                                                Toast.makeText(SelectPoi.this, poiName + " added to favourites!", Toast.LENGTH_SHORT).show();

                                            }else{
                                                Toast.makeText(SelectPoi.this, "Failed to update favourite POIs to database!", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }else{
                                    Toast.makeText(SelectPoi.this, "Failed to receive favourite POIs from database!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });

                convertView.setTag(viewHolder);
                mainViewholder = (ViewHolder) viewHolder;
            }else
                mainViewholder = (ViewHolder) convertView.getTag();

            mainViewholder.poiName.setText(getItem(position));

            for (String favPoiName : favPoiList){
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
        Button delete_btn;
        ImageView fav, unfav;
    }
}
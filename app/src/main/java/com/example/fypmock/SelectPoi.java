package com.example.fypmock;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SelectPoi extends AppCompatActivity implements MyDialogFragmentListener{

    FirebaseAuth mAuth;
    FirebaseDatabase mDatabase;

    private ArrayList<String> data = new ArrayList<String>();
    private ArrayList<String> favPoiList = new ArrayList<String>();
    private ListView lv;
    private SelectedPoiListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        setContentView(R.layout.activity_select_poi);
        lv = findViewById(R.id.lv_selectedPoi);
        Button add_btn = findViewById(R.id.btn_add);
        Button calRoute_btn = findViewById(R.id.btn_calRoute);

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
                if (data.size() == 0){
                    Toast.makeText(SelectPoi.this, "No POI selected!", Toast.LENGTH_SHORT).show();
                }else{
                    new NavMethodSelectionDialog().show(getSupportFragmentManager(), "NavigationMethodSelection");
                }
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
        mDatabase.getReference("Users")
                .child(mAuth.getCurrentUser().getUid()).child("favPoiNames")
                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                String [] tempList = task.getResult().getValue().toString().split(", ");
                Arrays.sort(tempList); //re-arrange list based on alphabet
                favPoiList.clear();
                favPoiList.addAll(Arrays.asList(tempList));

                if (!newPoiName.equals("")){
                    data.add(newPoiName);

                    Set<String> set = new LinkedHashSet<String>(data);
                    set.addAll(data);

                    SharedPreferences prefs = getSharedPreferences("SelectedPOIs", MODE_PRIVATE);
                    JSONArray jsonArray = new JSONArray(data);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("SelectedPOIsKey", jsonArray.toString());
                    editor.apply();
                    Toast.makeText(SelectPoi.this, newPoiName + " has been added to selected POIs!", Toast.LENGTH_LONG).show();
                }

                adapter.notifyDataSetChanged();
            }
        });
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

                    SharedPreferences prefs = getSharedPreferences("SelectedPOIs", MODE_PRIVATE);
                    JSONArray jsonArray = null;
                    try {
                        jsonArray = new JSONArray(prefs.getString("SelectedPOIsKey", null));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < jsonArray.length(); i++) {
                        try {
                            data.add(jsonArray.get(i).toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    adapter = new SelectedPoiListAdapter(SelectPoi.this, R.layout.item_with_btn, data);
                    lv.setAdapter(adapter);
                }else{
                    Toast.makeText(SelectPoi.this, "Failed to retrieve favourite POI names!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onReturnValue(String selectedMethod) {
        Intent intent = new Intent(SelectPoi.this, DisplayPoiOrder.class);
        intent.putExtra("SELECTED_METHOD", selectedMethod);
        intent.putExtra("SELECTED_POIS", data);
        startActivity(intent);
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

                convertView.setTag(viewHolder);
                mainViewholder = (ViewHolder) viewHolder;
            }else
                mainViewholder = (ViewHolder) convertView.getTag();

            mainViewholder.poiName.setText(getItem(position));
            ViewHolder finalMainViewholder = mainViewholder;

            mainViewholder.delete_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String poiName = data.get(position);
                    data.remove(position);

                    Set<String> set = new LinkedHashSet<String>(data);
                    set.addAll(data);

                    SharedPreferences prefs = getSharedPreferences("SelectedPOIs", MODE_PRIVATE);
                    JSONArray jsonArray = new JSONArray(data);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("SelectedPOIsKey", jsonArray.toString());
                    editor.apply();

                    notifyDataSetChanged();
                    Toast.makeText(SelectPoi.this, poiName + " has been removed!", Toast.LENGTH_SHORT).show();
                }
            });

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
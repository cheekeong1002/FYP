package com.example.fypmock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FavouriteFragment extends Fragment {

    FirebaseDatabase f;

    private FavPoiListAdapter adapter;
    private ArrayList<String> favPoiList = new ArrayList<>();
    private ListView mlv_fav;

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

        getFavPoi();
    }

    private void getFavPoi(){
        FirebaseDatabase.getInstance().getReference("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("favPoiNames")
                .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()){
                    String [] tempList = task.getResult().getValue().toString().split(", ");
                    Arrays.sort(tempList); //re-arrange list based on alphabet
                    favPoiList.addAll(Arrays.asList(tempList));

                    adapter = new FavPoiListAdapter(getActivity(), R.layout.item, favPoiList);
                    mlv_fav.setAdapter(adapter);
                    mlv_fav.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Intent data = new Intent();
                            data.putExtra("selectedPoi", favPoiList.get(position));
                            // Activity finished ok, return the data
                            getActivity().setResult(Activity.RESULT_OK, data);
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
                viewHolder.poiName = (TextView) convertView.findViewById(R.id.tv_username);
                viewHolder.fav = (ImageView) convertView.findViewById(R.id.fav);
                viewHolder.unfav = (ImageView) convertView.findViewById(R.id.unfav);

                viewHolder.fav.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String poiName = favPoiList.get(position);
                        viewHolder.unfav.setVisibility(View.VISIBLE);
                        viewHolder.fav.setVisibility(View.GONE);
                        Toast.makeText(getActivity(), poiName + " removed to favourites!", Toast.LENGTH_SHORT).show();
                    }
                });

                viewHolder.unfav.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String poiName = favPoiList.get(position);
                        viewHolder.fav.setVisibility(View.VISIBLE);
                        viewHolder.unfav.setVisibility(View.GONE);
                        Toast.makeText(getActivity(), poiName + " added to favourites!", Toast.LENGTH_SHORT).show();
                    }
                });

                convertView.setTag(viewHolder);
                mainViewholder = (ViewHolder) viewHolder;
            }else
                mainViewholder = (ViewHolder) convertView.getTag();

            mainViewholder.poiName.setText(getItem(position));

            return convertView;
        }
    }

    public class ViewHolder {
        TextView poiName;
        ImageView fav, unfav;
    }
}
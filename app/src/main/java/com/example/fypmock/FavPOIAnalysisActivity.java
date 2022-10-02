package com.example.fypmock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;

public class FavPOIAnalysisActivity extends AppCompatActivity {

    private FirebaseDatabase mDatabase;

    private PieChart mPieChart;
    private final ArrayList<String[]> userFavPoi = new ArrayList<>();
    private final ArrayList<String> allFavPoi = new ArrayList<>();
    private final ArrayList<Integer> favPoiCounter = new ArrayList<>();
    private final ArrayList<String[]> orderToDisplay = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_p_o_i_analysis);

        mDatabase = FirebaseDatabase.getInstance();

        mPieChart = findViewById(R.id.favPieChart);
        getAllFavouritePOI();
    }

    private void setupPieChart(){
        mPieChart.setDrawHoleEnabled(true);
        mPieChart.setEntryLabelTextSize(12);
        mPieChart.setEntryLabelColor(Color.WHITE);
        mPieChart.setCenterText("Top 5 Favourite POI");
        mPieChart.setCenterTextSize(24);
        mPieChart.getDescription().setEnabled(false);

        Legend l = mPieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setEnabled(true);
    }

    private void loadPieChartData(){
        int counter = 1;
        ArrayList<PieEntry> entries = new ArrayList<>();

        for (String[] favPoi: orderToDisplay){
            if (counter == 6){
                break;
            }
            entries.add(new PieEntry(Float.parseFloat(favPoi[0]), favPoi[1]));
            counter++;
        }

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#68BBE3"));
        colors.add(Color.parseColor("#0E86D4"));
        colors.add(Color.parseColor("#055C9D"));
        colors.add(Color.parseColor("#003060"));
        colors.add(Color.parseColor("#050A30"));

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        PieDataSet mDataSet = new PieDataSet(entries, "");
        mDataSet.setColors(colors);

        ValueFormatter vf = new ValueFormatter() { //value format here, here is the overridden method
            @Override
            public String getFormattedValue(float value) {
                return ""+(int)value;
            }
        };

        PieData data = new PieData(mDataSet);
        data.setValueFormatter(vf);
        data.setDrawValues(true);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.WHITE);

        mPieChart.setData(data);
        mPieChart.invalidate(); //notify pie chart has been updated

        mPieChart.animateY(1400, Easing.EaseInOutQuad);
    }

    private void countFavPoi(){
        int highestTotal = 0;
        int highestPOIPost = 0;

        for (String[] singleUserFav: userFavPoi){
            for (String singleFavPOI: singleUserFav){
                if (!allFavPoi.contains(singleFavPOI)){
                    allFavPoi.add(singleFavPOI);
                    favPoiCounter.add(1);
                }else{
                    for (int x = 0; x < allFavPoi.size(); x++){
                        if (allFavPoi.get(x).equals(singleFavPOI)){
                            int currentTotal = favPoiCounter.get(x);
                            favPoiCounter.set(x, currentTotal + 1);
                            break;
                        }
                    }
                }
            }
        }

        Log.d("TAG", "all poi: " + allFavPoi);
        Log.d("TAG", "all counter: " + favPoiCounter);

        while (favPoiCounter.size() != 0){
            for (int x = 0; x < favPoiCounter.size(); x++){
                if (favPoiCounter.get(x) > highestTotal){
                    highestTotal = favPoiCounter.get(x);
                    highestPOIPost = x;
                }
            }

            float totalPoi = favPoiCounter.get(highestPOIPost).floatValue();
            String favPoiName = allFavPoi.get(highestPOIPost);
            favPoiCounter.remove(highestPOIPost);
            allFavPoi.remove(highestPOIPost);
            highestPOIPost = 0;

            String[] tempArray = {Float.toString(totalPoi), favPoiName};
            orderToDisplay.add(tempArray);
        }

        setupPieChart();
        loadPieChartData();
    }

    private void getAllFavouritePOI(){
        mDatabase.getReference("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot: snapshot.getChildren()){
                    User userProfile = dataSnapshot.getValue(User.class);
                    if (!userProfile.getFavPoiNames().equals("")){
                        String[] favPoiNames = userProfile.getFavPoiNames().split(", ");
                        userFavPoi.add(favPoiNames);
                    }
                }

                if (userFavPoi.size() > 0){
                    countFavPoi();
                }else{
                    Toast.makeText(FavPOIAnalysisActivity.this, "No data available!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FavPOIAnalysisActivity.this, "Failed to get user data!", Toast.LENGTH_LONG).show();
            }
        });
    }
}
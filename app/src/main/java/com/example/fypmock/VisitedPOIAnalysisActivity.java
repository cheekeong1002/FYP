package com.example.fypmock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class VisitedPOIAnalysisActivity extends AppCompatActivity {

    private FirebaseDatabase mDatabase;

    private Spinner mSpinner;
    private BarChart mBarChart;
    private final ArrayList<Integer> topNumDisplay = new ArrayList<>();
    private final ArrayList<String[]> userVisitedPoi = new ArrayList<>();
    private final ArrayList<String> allVisitedPoi = new ArrayList<>();
    private final ArrayList<Integer> visitedPoiCounter = new ArrayList<>();
    private final ArrayList<String[]> orderToDisplay = new ArrayList<>();
    private final ArrayList<String> labelNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visited_p_o_i_analysis);

        mDatabase = FirebaseDatabase.getInstance();

        mSpinner = findViewById(R.id.topNumSelector);
        mBarChart = findViewById(R.id.visitedPoiBarChart);

        getAllVisitedPOI();
    }

    private void loadBarChartData(int totalFavToLoad){
        int counter = 0;
        ArrayList<BarEntry> barEntries = new ArrayList<>();

        for (String[] favPoi: orderToDisplay){
            if (counter == totalFavToLoad){
                break;
            }
            barEntries.add(new BarEntry(counter, Float.parseFloat(favPoi[0])));
            labelNames.add(favPoi[1]);
            counter++;
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Top Favourite POIs");

        ArrayList<Integer> barChartColors = new ArrayList<>();
        barChartColors.add(Color.parseColor("#68BBE3"));
        barChartColors.add(Color.parseColor("#0E86D4"));
        barChartColors.add(Color.parseColor("#055C9D"));
        barChartColors.add(Color.parseColor("#003060"));
        barChartColors.add(Color.parseColor("#050A30"));
        barDataSet.setColors(barChartColors);

        Description desc = new Description();
        desc.setText("POIs");
        mBarChart.setDescription(desc);

        BarData barData = new BarData(barDataSet);
        mBarChart.setData(barData);

        ValueFormatter vf = new ValueFormatter() { //value format here, here is the overridden method
            @Override
            public String getFormattedValue(float value) {
                return ""+(int)value;
            }
        };

        YAxis yAxisLeft = mBarChart.getAxisLeft();
        YAxis yAxisRight = mBarChart.getAxisRight();
        BarData mBarDate = mBarChart.getBarData();

        yAxisLeft.setValueFormatter(vf);
        yAxisRight.setValueFormatter(vf);
        mBarDate.setValueFormatter(vf);

        XAxis xAxis = mBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labelNames));

        xAxis.setPosition(XAxis.XAxisPosition.TOP);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labelNames.size());
        xAxis.setLabelRotationAngle(270);
        mBarChart.animateY(1400);
        mBarChart.invalidate();
    }

    private void countFavPoi(){
        int highestTotal = 0;
        int highestPOIPost = 0;

        for (String[] singleUserFav: userVisitedPoi){
            for (String singleFavPOI: singleUserFav){
                if (!allVisitedPoi.contains(singleFavPOI)){
                    allVisitedPoi.add(singleFavPOI);
                    visitedPoiCounter.add(1);
                }else{
                    for (int x = 0; x < allVisitedPoi.size(); x++){
                        if (allVisitedPoi.get(x).equals(singleFavPOI)){
                            int currentTotal = visitedPoiCounter.get(x);
                            visitedPoiCounter.set(x, currentTotal + 1);
                            break;
                        }
                    }
                }
            }
        }

        while (visitedPoiCounter.size() != 0 && orderToDisplay.size() <= 10){
            for (int x = 0; x < visitedPoiCounter.size(); x++){
                if (visitedPoiCounter.get(x) > highestTotal){
                    highestTotal = visitedPoiCounter.get(x);
                    highestPOIPost = x;
                }
            }

            float totalPoi = visitedPoiCounter.get(highestPOIPost).floatValue();
            String favPoiName = allVisitedPoi.get(highestPOIPost);
            visitedPoiCounter.remove(highestPOIPost);
            allVisitedPoi.remove(highestPOIPost);
            highestTotal = 0;
            highestPOIPost = 0;

            String[] tempArray = {Float.toString(totalPoi), favPoiName};
            orderToDisplay.add(tempArray);
        }

        for (int x=2; x < orderToDisplay.size(); x++){
            topNumDisplay.add(x);
        }

        ArrayAdapter<Integer> spinnerAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_dropdown_item, topNumDisplay);
        mSpinner.setAdapter(spinnerAdapter);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mBarChart.setVisibility(View.VISIBLE);
                loadBarChartData(position + 2);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void getAllVisitedPOI(){
        mDatabase.getReference("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot: snapshot.getChildren()){
                    User userProfile = dataSnapshot.getValue(User.class);
                    if (!userProfile.getFavPoiNames().equals("")){
                        String[] visitedPoiNames = userProfile.getFavPoiNames().split(", ");
                        userVisitedPoi.add(visitedPoiNames);
                    }
                }

                if (userVisitedPoi.size() > 1){
                    countFavPoi();
                }else{
                    Toast.makeText(VisitedPOIAnalysisActivity.this, "No data available!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(VisitedPOIAnalysisActivity.this, "Failed to get user data!", Toast.LENGTH_LONG).show();
            }
        });
    }
}
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
import java.util.Arrays;

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
    private int selectedItemPost;
    private boolean savedStateExists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visited_p_o_i_analysis);

        mDatabase = FirebaseDatabase.getInstance();

        mSpinner = findViewById(R.id.topNumSelector);
        mBarChart = findViewById(R.id.visitedPoiBarChart);

        getAllVisitedPOI();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("LastSelectedItemPost", selectedItemPost);
        savedInstanceState.putBoolean("SavedStateExists", true);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedItemPost = savedInstanceState.getInt("LastSelectedItemPost");
        savedStateExists = savedInstanceState.getBoolean("SavedStateExists");
    }

    private void loadBarChartData(int totalFavToLoad){
        int counter = 0;
        labelNames.clear();
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

        BarData mBarDate = mBarChart.getBarData();
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

    private void countVisitedPoi(){
        int highestTotal = 0;
        int highestPOIPost = 0;

        for (String[] singleUserVisited: userVisitedPoi){
            for (String singleVisitedPOI: singleUserVisited){
                if (!allVisitedPoi.contains(singleVisitedPOI)){
                    allVisitedPoi.add(singleVisitedPOI);
                    visitedPoiCounter.add(1);
                }else{
                    for (int x = 0; x < allVisitedPoi.size(); x++){
                        if (allVisitedPoi.get(x).equals(singleVisitedPOI)){
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
            Log.d("TAG", "countVisitedPoi: " + Arrays.toString(tempArray));
            orderToDisplay.add(tempArray);
        }

        for (int x=1; x < orderToDisplay.size() + 1; x++){
            topNumDisplay.add(x);
        }

        ArrayAdapter<Integer> spinnerAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_dropdown_item, topNumDisplay);
        mSpinner.setAdapter(spinnerAdapter);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mBarChart.setVisibility(View.VISIBLE);

                if (savedStateExists){
                    mSpinner.setSelection(selectedItemPost - 1);
                    loadBarChartData(selectedItemPost);
                    savedStateExists = false;
                }else{
                    loadBarChartData(position + 1);
                    selectedItemPost = position + 1;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void getAllVisitedPOI(){
        mDatabase.getReference("History").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot: snapshot.getChildren()){
                    int counter = 0;
                    String tempArray[] = new String[(int) dataSnapshot.getChildrenCount()];

                    for (DataSnapshot data: dataSnapshot.getChildren()){
                        tempArray[counter] = (String) data.getValue();
                        counter++;
                    }

                    userVisitedPoi.add(tempArray);
                }

                if (userVisitedPoi.size() > 1){
                    countVisitedPoi();
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
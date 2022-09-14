package com.example.fypmock;
import androidx.appcompat.app.AppCompatActivity;

public abstract class GetBuildingID extends AppCompatActivity {

    //set the building id as static
    public static final String BUILDING_ID = "11357";

    protected String getBuildingID() {
        return BUILDING_ID;
    }

}

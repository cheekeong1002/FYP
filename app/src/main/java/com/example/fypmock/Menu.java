package com.example.fypmock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

public class Menu extends AppCompatActivity {

    FirebaseAuth mAuth;

    private AppBarConfiguration mAppBarConfiguration;
    private FirebaseUser user;
    private DatabaseReference ref;
    private String userID, userType;
    private TextView mtv_guideAdmin, mtv_guideUser;
    private Button toVisitedAnalysis_btn, toSelectPoi_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null){
            Intent intent = new Intent(Menu.this, LoginActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Please login to continue!", Toast.LENGTH_SHORT).show();
            return;
        }

        user = mAuth.getCurrentUser();
        ref = FirebaseDatabase.getInstance().getReference("Users");
        userID = user.getUid();

        mtv_guideUser = findViewById(R.id.tv_guideUser);
        toSelectPoi_btn = findViewById(R.id.btn_toSelectPoi);
        mtv_guideAdmin = findViewById(R.id.tv_guideAdmin);
        toVisitedAnalysis_btn = findViewById(R.id.btn_toVisitedAnalysis);

        ref.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userProfile = snapshot.getValue(User.class);

                if (userProfile != null){
                    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                    View headerView = navigationView.getHeaderView(0);
                    TextView navUsername = (TextView) headerView.findViewById(R.id.tv_greeting_username);
                    navUsername.setText(userProfile.username);
                    userType = userProfile.getType();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Menu.this, "Failed to get user data!", Toast.LENGTH_LONG).show();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        if (userType == null){
            ref.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User userProfile = snapshot.getValue(User.class);

                    if (userProfile != null){
                        userType = userProfile.getType();
                        if (!userType.equals("user")){
                            for (int x = 1; x < 5; x++){
                                navigationView.getMenu().getItem(x).setVisible(false);
                            }
                            navigationView.getMenu().getItem(5).setVisible(true);
                            navigationView.getMenu().getItem(6).setVisible(true);

                            if (userType.equals("adminHigh")){
                                navigationView.getMenu().getItem(7).setVisible(true);
                            }

                            mtv_guideUser.setVisibility(View.GONE);
                            toSelectPoi_btn.setVisibility(View.GONE);
                            mtv_guideAdmin.setVisibility(View.VISIBLE);
                            toVisitedAnalysis_btn.setVisibility(View.VISIBLE);
                        }else{
                            mtv_guideUser.setVisibility(View.VISIBLE);
                            toSelectPoi_btn.setVisibility(View.VISIBLE);
                            mtv_guideAdmin.setVisibility(View.GONE);
                            toVisitedAnalysis_btn.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(Menu.this, "Failed to get user data!", Toast.LENGTH_LONG).show();
                }
            });
        }else{
            if (!userType.equals("user")){
                for (int x = 1; x < 5; x++){
                    navigationView.getMenu().getItem(x).setVisible(false);
                }
                navigationView.getMenu().getItem(5).setVisible(true);
                navigationView.getMenu().getItem(6).setVisible(true);

                if (userType.equals("adminHigh")){
                    navigationView.getMenu().getItem(7).setVisible(true);
                }

                mtv_guideUser.setVisibility(View.GONE);
                toSelectPoi_btn.setVisibility(View.GONE);
                mtv_guideAdmin.setVisibility(View.VISIBLE);
                toVisitedAnalysis_btn.setVisibility(View.VISIBLE);
            }else{
                mtv_guideUser.setVisibility(View.VISIBLE);
                toSelectPoi_btn.setVisibility(View.VISIBLE);
                mtv_guideAdmin.setVisibility(View.GONE);
                toVisitedAnalysis_btn.setVisibility(View.GONE);
            }
        }

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;

                switch (item.toString()){
                    case "Profile":
                        intent = new Intent(Menu.this, ViewProfileActivity.class);
                        startActivity(intent);
                        break;

                    case "Favourite POIs":
                        intent = new Intent(Menu.this, FavouritePoiActivity.class);
                        startActivity(intent);
                        break;

                    case "View Map":
                        intent = new Intent(Menu.this, Map.class);
                        startActivity(intent);
                        break;

                    case "Plan Route":
                        intent = new Intent(Menu.this, SelectPoi.class);
                        startActivity(intent);
                        break;

                    case "Favourite POI Analysis":
                        intent = new Intent(Menu.this, FavPOIAnalysisActivity.class);
                        startActivity(intent);
                        break;

                    case "Visited POI Analysis":
                        intent = new Intent(Menu.this, VisitedPOIAnalysisActivity.class);
                        startActivity(intent);
                        break;

                    case "Create Admin":
                        intent = new Intent(Menu.this, CreateAdminActivity.class);
                        startActivity(intent);
                        break;

                    case "Logout":
                        mAuth.signOut();

                        intent = new Intent(Menu.this, LoginActivity.class);
                        startActivity(intent);
                        break;

                }
                return false;
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
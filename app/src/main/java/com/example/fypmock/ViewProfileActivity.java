package com.example.fypmock;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ViewProfileActivity extends AppCompatActivity implements View.OnClickListener{

    FirebaseAuth mAuth;
    FirebaseDatabase mDatabase;
    TextView mtv_username, mtv_email, mtv_password;
    ImageButton username_btn, email_btn, password_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        mtv_username = findViewById(R.id.tv_username);
        mtv_email = findViewById(R.id.tv_email);
        mtv_password = findViewById(R.id.tv_password);

        username_btn = findViewById(R.id.btn_username);
        username_btn.setOnClickListener(this);

        email_btn = findViewById(R.id.btn_email);
        email_btn.setOnClickListener(this);

        password_btn = findViewById(R.id.btn_password);
        password_btn.setOnClickListener(this);

        mDatabase.getReference("Users")
                .child(mAuth.getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        mtv_username.setText(user.getUsername());
                        mtv_email.setText(user.getEmail());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ViewProfileActivity.this, "Failed to receieve data from database", Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()){
            case R.id.btn_username:
                intent = new Intent(ViewProfileActivity.this, ModifyProfileActivity.class);
                intent.putExtra("MODIFY_AREA", "Username");
                intent.putExtra("DATA", mtv_username.getText());
                resultLauncher.launch(intent);
                break;

            case R.id.btn_email:
                intent = new Intent(ViewProfileActivity.this, ModifyProfileActivity.class);
                intent.putExtra("MODIFY_AREA", "Email");
                intent.putExtra("DATA", mtv_email.getText());
                resultLauncher.launch(intent);
                break;

            case R.id.btn_password:
                intent = new Intent(ViewProfileActivity.this, ModifyProfileActivity.class);
                intent.putExtra("MODIFY_AREA", "Password");
                intent.putExtra("DATA", mtv_email.getText());
                resultLauncher.launch(intent);
                break;
        }
    }

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        String modificationArea = data.getStringExtra("MODIFY_AREA");
                        Toast.makeText(ViewProfileActivity.this, modificationArea, Toast.LENGTH_SHORT).show();
                        String info = data.getStringExtra("DATA");
                        if (!modificationArea.equals("")){
                            switch (modificationArea){
                                case "Username":
                                    mtv_username.setText(info);
                                    Log.d("ViewProfileActivity", "Username has been modified!");
                                    break;

                                case "Email":
                                    mtv_email.setText(info);
                                    Log.d("ViewProfileActivity", "Email has been modified!");
                                    break;

                                case "Password":
                                    Log.d("ViewProfileActivity", "Password has been modified!");
                                    break;
                            }
                        }
                    }
                }
            });
}
package com.example.fypmock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class CreateAdminActivity extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth mAuth;
    private EditText met_username, met_email, met_password, met_rePassword;
    private Button create_btn;
    private ProgressBar mProgressBar;
    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_admin);

        mAuth = FirebaseAuth.getInstance();

        create_btn = findViewById(R.id.btn_create);
        create_btn.setOnClickListener(this);

        List<String> spinnerArray =  new ArrayList<String>();
        spinnerArray.add("Privilege");
        spinnerArray.add("High");
        spinnerArray.add("Low");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerArray){

            @Override
            public boolean isEnabled(int position) {
                if (position > 0){
                    return true;
                }else{
                    return false;
                }
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View mView = super.getDropDownView(position, convertView, parent);
                TextView mTextView = (TextView) mView;
                if (position > 0) {
                    mTextView.setTextColor(Color.BLACK);
                } else {
                    mTextView.setTextColor(Color.GRAY);
                }
                return mView;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner = findViewById(R.id.sp_adminPrivilege);
        mSpinner.setAdapter(adapter);

        met_username = findViewById(R.id.et_username);
        met_email = findViewById(R.id.et_email);
        met_password = findViewById(R.id.et_password);
        met_rePassword = findViewById(R.id.et_rePassword);
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_create:
                createAdmin();
                break;
        }
    }

    private void createAdmin(){
        String username = met_username.getText().toString().trim();
        String email = met_email.getText().toString().trim();
        String password = met_password.getText().toString().trim();
        String rePassword = met_rePassword.getText().toString().trim();
        String privilege = mSpinner.getSelectedItem().toString().trim();

        if(username.isEmpty()){
            met_username.setError("Username is required!");
            met_username.requestFocus();
            return;
        }

        if(email.isEmpty()){
            met_email.setError("Email is required!");
            met_email.requestFocus();
            return;
        }

        if(password.isEmpty()){
            met_password.setError("Password is required!");
            met_password.requestFocus();
            return;
        }

        if(rePassword.isEmpty()){
            met_rePassword.setError("Retype password is required!");
            met_rePassword.requestFocus();
            return;
        }

        if(username.length() > 10){
            met_username.setError("Username not be more than 10 characters!");
            met_username.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            met_email.setError("Email format is invalid!");
            met_email.requestFocus();
            return;
        }

        if (password.length() < 6){
            met_password.setError("Password should not be less than 6!");
            met_password.requestFocus();
            return;
        }

        if (rePassword.length() < 6){
            met_rePassword.setError("Retype password should not be less than 6!");
            met_rePassword.requestFocus();
            return;
        }

        if (!password.equals(rePassword)){
            met_rePassword.setError("Passwords do not match!");
            met_rePassword.requestFocus();
            return;
        }

        if (privilege.equals("Privilege")){
            TextView errorText = (TextView)mSpinner.getSelectedView();
            errorText.setError("");
            errorText.setTextColor(Color.RED);
            errorText.setText("Select a privilege!");
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            String adminType = "";

                            if (privilege.equals("High")){
                                adminType = "adminHigh";
                            }else if (privilege.equals("Low")){
                                adminType = "adminLow";
                            }

                            User user = new User(username, email, adminType, "");

                            FirebaseDatabase.getInstance().getReference("Users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        mProgressBar.setVisibility(View.GONE);
                                        Toast.makeText(CreateAdminActivity.this, "Successfully created admin account!", Toast.LENGTH_LONG).show();
                                    }else{
                                        Toast.makeText(CreateAdminActivity.this, "Failed to create admin account!", Toast.LENGTH_LONG).show();
                                        mProgressBar.setVisibility(View.GONE);
                                    }
                                }
                            });
                        }else{
                            Toast.makeText(CreateAdminActivity.this, "Failed to create admin account!", Toast.LENGTH_LONG).show();
                            mProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }
}
package com.example.fypmock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth mAuth;
    private EditText met_username, met_email, met_password;
    private TextView mtv_toLogin;
    private Button register_btn;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        mtv_toLogin = findViewById(R.id.tv_toLogin);
        mtv_toLogin.setOnClickListener(this);

        register_btn = findViewById(R.id.btn_register);
        register_btn.setOnClickListener(this);

        met_username = findViewById(R.id.et_username);
        met_email = findViewById(R.id.et_email);
        met_password = findViewById(R.id.et_password);
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_toLogin:
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_register:
                registerUser();
                break;
        }
    }

    private void registerUser(){
        String username = met_username.getText().toString().trim();
        String email = met_email.getText().toString().trim();
        String password = met_password.getText().toString().trim();

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

        mProgressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            User user = new User(username, email, "user", "");

                            FirebaseDatabase.getInstance().getReference("Users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        mProgressBar.setVisibility(View.GONE);
                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        Toast.makeText(RegisterActivity.this, "Success! Please login to the new account!", Toast.LENGTH_LONG).show();
                                    }else{
                                        Toast.makeText(RegisterActivity.this, "Failed to register an account!", Toast.LENGTH_LONG).show();
                                        mProgressBar.setVisibility(View.GONE);
                                    }
                                }
                            });
                        }else{
                            Toast.makeText(RegisterActivity.this, "Failed to register an account!", Toast.LENGTH_LONG).show();
                            mProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }
}
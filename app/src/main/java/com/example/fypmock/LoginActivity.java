package com.example.fypmock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth mAuth;
    private EditText met_email, met_password;
    private TextView mtv_toRegister, mtv_forgotPassword;
    private Button login_btn;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null){
            Intent intent = new Intent(LoginActivity.this, Menu.class);
            startActivity(intent);
        }

        mtv_toRegister = findViewById(R.id.tv_toRegister);
        mtv_toRegister.setOnClickListener(this);

        mtv_forgotPassword = findViewById(R.id.tv_forgotPassword);
        mtv_forgotPassword.setOnClickListener(this);

        login_btn = findViewById(R.id.btn_login);
        login_btn.setOnClickListener(this);

        met_email = findViewById(R.id.et_email);
        met_password = findViewById(R.id.et_password);
        mProgressBar = findViewById(R.id.progressBar);
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()){
            case R.id.tv_forgotPassword:
                intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
                break;

            case R.id.tv_toRegister:
                intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_login:
                loginUser();
        }
    }

    private void loginUser(){
        String email = met_email.getText().toString().trim();
        String password = met_password.getText().toString().trim();

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

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                    prefs.edit().putBoolean("IsLogin", true).apply();

                    Intent intent = new Intent(LoginActivity.this, Menu.class);
                    startActivity(intent);
                    mProgressBar.setVisibility(View.GONE);
                }else{
                    Toast.makeText(LoginActivity.this, "Failed to login! Check your credentials!", Toast.LENGTH_LONG).show();
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}
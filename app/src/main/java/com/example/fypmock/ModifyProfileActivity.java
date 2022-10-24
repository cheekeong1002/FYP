package com.example.fypmock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class ModifyProfileActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseDatabase mDatabase;

    LinearLayout normalLayout, reAuthLayout;
    String modificationArea, data;
    TextView mtv_title, mtv_newEmail, mtv_newPassword, mtv_rePassword;
    EditText met_input, met_email, met_password, met_newEmail, met_newPassword, met_rePassword;
    Button saveChanges_btn, cancel_btn;
    ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        normalLayout = findViewById(R.id.normal_layout);
        reAuthLayout = findViewById(R.id.reAuth_layout);
        mtv_title = findViewById(R.id.tv_title);
        met_input = findViewById(R.id.et_input);
        mProgressBar = findViewById(R.id.progressBar);

        mtv_newEmail = findViewById(R.id.tv_newEmail);
        mtv_newPassword = findViewById(R.id.tv_newPassword);
        mtv_rePassword = findViewById(R.id.tv_rePassword);
        met_email = findViewById(R.id.et_email);
        met_password = findViewById(R.id.et_password);
        met_newEmail = findViewById(R.id.et_newEmail);
        met_newPassword = findViewById(R.id.et_newPassword);
        met_rePassword = findViewById(R.id.et_rePassword);

        modificationArea = getIntent().getStringExtra("MODIFY_AREA");
        data = getIntent().getStringExtra("DATA");

        cancel_btn = findViewById(R.id.btn_cancel);
        saveChanges_btn = findViewById(R.id.btn_saveChanges);
        saveChanges_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (modificationArea){
                    case "Username":
                        updateUsername();
                        break;
                    case "Email":
                        updateEmail();
                        break;
                    case "Password":
                        updatePassword();
                        break;
                }
            }
        });

        Button cancel_btn = findViewById(R.id.btn_cancel);
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("MODIFY_AREA", "");
                finish();
            }
        });

        if (modificationArea.equals("Email")){
            normalLayout.setVisibility(View.GONE);
            reAuthLayout.setVisibility(View.VISIBLE);
            mtv_newEmail.setVisibility(View.VISIBLE);
            met_newEmail.setVisibility(View.VISIBLE);
        }

        if (modificationArea.equals("Password")){
            normalLayout.setVisibility(View.GONE);
            reAuthLayout.setVisibility(View.VISIBLE);
            mtv_newPassword.setVisibility(View.VISIBLE);
            met_newPassword.setVisibility(View.VISIBLE);
            mtv_rePassword.setVisibility(View.VISIBLE);
            met_rePassword.setVisibility(View.VISIBLE);
        }

        updateUI();
    }

    public void updatePassword(){
        String email = met_email.getText().toString().trim();
        String password = met_password.getText().toString().trim();
        String newPassword = met_newPassword.getText().toString().trim();
        String rePassword = met_rePassword.getText().toString().trim();

        if(email.isEmpty()){
            met_email.setError("Email is required!");
            met_email.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            met_email.setError("Email format is invalid!");
            met_email.requestFocus();
            return;
        }

        if(password.isEmpty()){
            met_password.setError("Password is required!");
            met_password.requestFocus();
            return;
        }

        if (password.length() < 6){
            met_password.setError("Password should not be less than 6!");
            met_password.requestFocus();
            return;
        }

        if(newPassword.isEmpty()){
            met_newPassword.setError("New password is required!");
            met_newPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6){
            met_newPassword.setError("New Password should not be less than 6!");
            met_newPassword.requestFocus();
            return;
        }

        if(rePassword.isEmpty()){
            met_rePassword.setError("Retype password is required!");
            met_rePassword.requestFocus();
            return;
        }

        if (rePassword.length() < 6){
            met_rePassword.setError("Retype Password should not be less than 6!");
            met_rePassword.requestFocus();
            return;
        }

        if (!newPassword.equals(rePassword)){
            met_rePassword.setError("Passwords do not match!");
            met_rePassword.requestFocus();
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        saveChanges_btn.setEnabled(false);
        cancel_btn.setEnabled(false);

        FirebaseUser user = mAuth.getCurrentUser();
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    FirebaseUser user = mAuth.getCurrentUser();
                    user.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Intent intent = new Intent();
                                intent.putExtra("MODIFY_AREA", "Password");
                                mProgressBar.setVisibility(View.GONE);
                                saveChanges_btn.setEnabled(true);
                                cancel_btn.setEnabled(true);
                                finish();
                                Toast.makeText(ModifyProfileActivity.this, modificationArea +
                                        " updated successfully!", Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(ModifyProfileActivity.this, "Failed to update password!", Toast.LENGTH_LONG).show();
                                mProgressBar.setVisibility(View.GONE);
                                saveChanges_btn.setEnabled(true);
                                cancel_btn.setEnabled(true);
                            }
                        }
                    });
                }else{
                    Toast.makeText(ModifyProfileActivity.this, "Failed to authenticate user! Try again!", Toast.LENGTH_LONG).show();
                    mProgressBar.setVisibility(View.GONE);
                    saveChanges_btn.setEnabled(true);
                    cancel_btn.setEnabled(true);
                }
            }
        });
    }

    public void updateEmail(){
        String email = met_email.getText().toString().trim();
        String password = met_password.getText().toString().trim();
        String newEmail = met_newEmail.getText().toString().trim();

        if(email.isEmpty()){
            met_email.setError("Email is required!");
            met_email.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            met_email.setError("Email format is invalid!");
            met_email.requestFocus();
            return;
        }

        if(password.isEmpty()){
            met_password.setError("Password is required!");
            met_password.requestFocus();
            return;
        }

        if (password.length() < 6){
            met_password.setError("Password should not be less than 6!");
            met_password.requestFocus();
            return;
        }

        if(newEmail.isEmpty()){
            met_newEmail.setError("New email is required!");
            met_newEmail.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()){
            met_newEmail.setError("New email format is invalid!");
            met_newEmail.requestFocus();
            return;
        }

        if (newEmail.equals(data)){
            met_newEmail.setError("No changes made!");
            met_newEmail.requestFocus();
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        saveChanges_btn.setEnabled(false);
        cancel_btn.setEnabled(false);

        FirebaseUser user = mAuth.getCurrentUser();
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    FirebaseUser user = mAuth.getCurrentUser();
                    user.updateEmail(newEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                mDatabase.getReference("Users")
                                        .child(mAuth.getCurrentUser().getUid()).child("email")
                                        .setValue(newEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            Intent intent = new Intent();
                                            intent.putExtra("MODIFY_AREA", "Email");
                                            intent.putExtra("DATA", newEmail);
                                            mProgressBar.setVisibility(View.GONE);
                                            saveChanges_btn.setEnabled(true);
                                            cancel_btn.setEnabled(true);
                                            finish();
                                            Toast.makeText(ModifyProfileActivity.this, modificationArea +
                                                    " updated successfully!", Toast.LENGTH_LONG).show();
                                        }else{
                                            Toast.makeText(ModifyProfileActivity.this, "Failed to update email!", Toast.LENGTH_LONG).show();
                                            mProgressBar.setVisibility(View.GONE);
                                            saveChanges_btn.setEnabled(true);
                                            cancel_btn.setEnabled(true);
                                        }
                                    }
                                });
                            }else{
                                Toast.makeText(ModifyProfileActivity.this, "Failed to update email!", Toast.LENGTH_LONG).show();
                                mProgressBar.setVisibility(View.GONE);
                                saveChanges_btn.setEnabled(true);
                                cancel_btn.setEnabled(true);
                            }
                        }
                    });
                }else{
                    Toast.makeText(ModifyProfileActivity.this, "Failed to authenticate user! Try again!", Toast.LENGTH_LONG).show();
                    mProgressBar.setVisibility(View.GONE);
                    saveChanges_btn.setEnabled(true);
                    cancel_btn.setEnabled(true);
                }
            }
        });
    }

    public void updateUsername(){
        String username = met_input.getText().toString().trim();

        if(username.isEmpty()){
            met_input.setError("Username is required!");
            met_input.requestFocus();
            return;
        }

        if(username.length() > 10){
            met_input.setError("Username not be more than 10 characters!");
            met_input.requestFocus();
            return;
        }

        if (username.equals(data)){
            met_input.setError("No changes made!");
            met_input.requestFocus();
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        saveChanges_btn.setEnabled(false);
        cancel_btn.setEnabled(false);

        mDatabase.getReference("Users")
                .child(mAuth.getCurrentUser().getUid()).child("username")
                .setValue(username).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Intent intent = new Intent();
                    intent.putExtra("MODIFY_AREA", "Username");
                    intent.putExtra("DATA", username);
                    mProgressBar.setVisibility(View.GONE);
                    saveChanges_btn.setEnabled(true);
                    cancel_btn.setEnabled(true);
                    finish();
                    Toast.makeText(ModifyProfileActivity.this, modificationArea +
                            " updated successfully!", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(ModifyProfileActivity.this, "Failed to update " + modificationArea + "!", Toast.LENGTH_LONG).show();
                    mProgressBar.setVisibility(View.GONE);
                    saveChanges_btn.setEnabled(true);
                    cancel_btn.setEnabled(true);
                }
            }
        });
    }

    private void updateUI(){
        if (modificationArea.equals("Email") || modificationArea.equals("Password")){
            met_email.setText(data);
        }else{
            mtv_title.setText(modificationArea);
            met_input.setText(data);

        }
    }
}
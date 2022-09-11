package com.example.fypmock;

import java.util.ArrayList;

public class User {
    public String username, email, type, selectedPoiNames;

    public User(){
    }

    public User(String username, String email, String type, String selectedPoiNames){
        this.username = username;
        this.email = email;
        this.type = type;
        this.selectedPoiNames = selectedPoiNames;
    }
}

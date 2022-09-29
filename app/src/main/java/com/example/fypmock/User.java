package com.example.fypmock;

import java.util.ArrayList;

public class User {
    public String username, email, type, favPoiNames;

    public User(){
    }

    public User(String username, String email, String type, String favPoiNames){
        this.username = username;
        this.email = email;
        this.type = type;
        this.favPoiNames = favPoiNames;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getType() {
        return type;
    }

    public String getFavPoiNames() {
        return favPoiNames;
    }
}

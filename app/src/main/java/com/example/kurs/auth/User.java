package com.example.kurs.auth;

public class User {
    public String email;

    public User() {
        // Пустой конструктор нужен Firebase
    }

    public User(String email) {
        this.email = email;
    }
}


package com.se.sebtl.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sso_users") // Matches your Supabase table name
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private AppRole role;

    public AppUser() {}

    public AppUser(String name, String username, String password, AppRole role) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Standard Getters and Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public AppRole getRole() { return role; }
    public void setRole(AppRole role) { this.role = role; }
}
package com.se.sebtl.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer alertId;

    @Enumerated(EnumType.STRING)
    private AlertType type;

    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();
    private boolean acknowledged = false;

    // Getters and Setters
}
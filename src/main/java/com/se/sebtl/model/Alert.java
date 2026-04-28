package com.se.sebtl.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Integer alertId;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private AlertType type;

    @Column(name = "message")
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @Column(name = "timestamp", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @Column(name = "acknowledged")
    private Boolean acknowledged = false;

    // Getters and Setters
    public Integer getAlertId() {
        return alertId;
    }
    public void setAlertId(Integer alertId) {
        this.alertId = alertId;
    }
    public AlertType getType() {
        return type;
    }
    public void setType(AlertType type) {
        this.type = type;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
    public Boolean isAcknowledged() {
        return acknowledged;
    }
    public void setAcknowledged(Boolean acknowledged) {
        this.acknowledged = acknowledged;
    }
}
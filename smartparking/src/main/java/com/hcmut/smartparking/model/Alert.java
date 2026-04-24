package com.hcmut.smartparking.model;

import java.util.*;
import com.hcmut.smartparking.enums.AlertType;

public class Alert {
    private int alertId;
    private AlertType type;
    private String message;
    private Date timestamp;
    private boolean acknowledged;
    
    public Alert() {}
    public Alert(AlertType type, String message) {
        this.type = type;
        this.message = message;
        this.timestamp = new Date();
        this.acknowledged = false;
    }

    // getters and setters
    public int getAlertId()             { return alertId; }
    public void setAlertId(int alertId) { this.alertId = alertId; }

    public AlertType getType()              { return type; }
    public void setType(AlertType type)     { this.type = type; }

    public String getMessage()                 { return message; }
    public void setMessage(String message)     { this.message = message; }

    public Date getTimestamp()                 { return timestamp; }
    public void setTimestamp(Date timestamp)   { this.timestamp = timestamp; }

    public boolean isAcknowledged()                    { return acknowledged; }
    public void setAcknowledged(boolean acknowledged)  { this.acknowledged = acknowledged; }
}
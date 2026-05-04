package com.se.sebtl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "price")
public class Price {
    @Id
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(name = "slot_priority")
    private Role priority;

    @Column(name = "price", nullable = false)
    private java.math.BigDecimal price = java.math.BigDecimal.ZERO;

    public Role getPriority() { return priority; }
    public void setPriority(Role priority) { this.priority = priority; }

    public java.math.BigDecimal getPrice() {
        return price;
    }
    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }
}

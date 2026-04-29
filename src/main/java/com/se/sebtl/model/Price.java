package com.se.sebtl.model;

import jakarta.persistence.*;

@Entity
@Table(name = "price")
public class Price {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "price", nullable = false)
    private java.math.BigDecimal price = java.math.BigDecimal.ZERO;

    public java.math.BigDecimal getPrice() {
        return price;
    }
    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }
}

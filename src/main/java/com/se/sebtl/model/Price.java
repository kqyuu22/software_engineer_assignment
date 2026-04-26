package com.se.sebtl.model;

import jakarta.persistence.*;

@Entity
@Table(name = "price")
public class Price {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "price")
    private Double price;

    public Double getPrice() {
        return price;
    }
    public void setPrice(Double price) {
        this.price = price;
    }
}

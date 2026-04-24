package com.hcmut.smartparking.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PriceDatabase {

    private final JdbcTemplate jdbc;

    @Autowired
    public PriceDatabase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public double getPrice() {
        Double price = jdbc.queryForObject(
            "SELECT price FROM price WHERE id = 1",
            Double.class
        );
        return price != null ? price : -1;
    }

    public void setPrice(double newPrice) {
        jdbc.update(
            "UPDATE price SET price = ? WHERE id = 1",
            newPrice
        );
    }
}
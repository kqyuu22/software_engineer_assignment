package com.hcmut.smartparking.database; 

import java.sql.Connection;
import java.sql.DriverManager;

public class DbConfig {
    private static final String URL      = "jdbc:postgresql://db.ojpufyapslzmygitujzj.supabase.co:5432/postgres";
    private static final String USER     = "postgres";
    private static final String PASSWORD = "juanvan2006@";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

//legacy class, will refactor later
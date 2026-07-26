package com.krishi.backend.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String testDbName = "krishi_test_db";
        String masterUrl = "jdbc:postgresql://localhost:5432/postgres";
        String username = System.getenv("SPRING_DATASOURCE_USERNAME") != null 
                ? System.getenv("SPRING_DATASOURCE_USERNAME") : "postgres";
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD") != null 
                ? System.getenv("SPRING_DATASOURCE_PASSWORD") : "Siva@2k5";

        try (Connection conn = DriverManager.getConnection(masterUrl, username, password);
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + testDbName + "'");
            if (!rs.next()) {
                stmt.executeUpdate("CREATE DATABASE " + testDbName);
                System.out.println("Created test database: " + testDbName);
            }
        } catch (Exception e) {
            System.err.println("Could not auto-create test database " + testDbName + ": " + e.getMessage());
        }
    }
}

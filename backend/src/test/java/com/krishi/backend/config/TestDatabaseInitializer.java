package com.krishi.backend.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String TEST_DATABASE_URL = "jdbc:postgresql://localhost:5432/krishi_test_db";
    private static final String ADMIN_DATABASE_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String TEST_DATABASE_NAME = "krishi_test_db";

    @FunctionalInterface
    interface DatabaseProvisioner {
        void createDatabaseIfMissing(String username, String password) throws Exception;
    }

    private final DatabaseProvisioner databaseProvisioner;

    public TestDatabaseInitializer() {
        this(TestDatabaseInitializer::createDatabaseIfMissing);
    }

    TestDatabaseInitializer(DatabaseProvisioner databaseProvisioner) {
        this.databaseProvisioner = databaseProvisioner;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        validateDatasourceUrl(environment.getProperty("spring.datasource.url"));

        String username = environment.getProperty("SPRING_DATASOURCE_USERNAME", "postgres");
        if (username.isBlank()) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_USERNAME must be non-blank for backend integration tests.");
        }

        String password = environment.getProperty("SPRING_DATASOURCE_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_PASSWORD must be set and non-blank for backend integration tests.");
        }

        try {
            databaseProvisioner.createDatabaseIfMissing(username, password);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not initialize the dedicated local test database 'krishi_test_db'. "
                            + "Check PostgreSQL at localhost:5432 and the test account's connection/creation permissions. "
                            + "Cause type: " + exception.getClass().getSimpleName() + ".");
        }
    }

    static void validateDatasourceUrl(String datasourceUrl) {
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            throw new IllegalStateException(
                    "spring.datasource.url is required and must target " + TEST_DATABASE_URL + ".");
        }
        if (!TEST_DATABASE_URL.equals(datasourceUrl.trim())) {
            throw new IllegalStateException(
                    "Backend integration tests may connect only to " + TEST_DATABASE_URL
                            + ". Check SPRING_TEST_DATASOURCE_URL.");
        }
    }

    private static void createDatabaseIfMissing(String username, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(ADMIN_DATABASE_URL, username, password);
             PreparedStatement query = connection.prepareStatement(
                     "SELECT 1 FROM pg_database WHERE datname = ?")) {
            query.setString(1, TEST_DATABASE_NAME);
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("CREATE DATABASE " + TEST_DATABASE_NAME);
                    }
                }
            }
        }
    }
}

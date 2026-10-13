package com.krishi.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDatabaseInitializerTest {

    @Test
    void acceptsOnlyTheApprovedLocalTestDatabaseAndRunsProvisioner() {
        AtomicInteger provisionerCalls = new AtomicInteger();
        TestDatabaseInitializer initializer = new TestDatabaseInitializer((username, password) ->
                provisionerCalls.incrementAndGet());

        initializer.initialize(context(TestDatabaseInitializer.TEST_DATABASE_URL));

        assertThat(provisionerCalls).hasValue(1);
    }

    @Test
    void resolvesApprovedDefaultUrlFromTestConfigurationWithoutConnecting() {
        AtomicInteger provisionerCalls = new AtomicInteger();
        AtomicReference<String> effectiveDatasourceUrl = new AtomicReference<>();
        TestDatabaseInitializer initializer = new TestDatabaseInitializer((username, password) ->
                provisionerCalls.incrementAndGet());

        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withInitializer((ConfigurableApplicationContext context) -> {
                    effectiveDatasourceUrl.set(
                            context.getEnvironment().getProperty("spring.datasource.url"));
                    initializer.initialize(context);
                })
                .withPropertyValues("SPRING_DATASOURCE_PASSWORD=" + UUID.randomUUID())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(effectiveDatasourceUrl.get())
                            .isEqualTo(TestDatabaseInitializer.TEST_DATABASE_URL);
                    assertThat(provisionerCalls).hasValue(1);
                });
    }

    @Test
    void rejectsNonTestDatabaseBeforeProvisioning() {
        AtomicInteger provisionerCalls = new AtomicInteger();
        TestDatabaseInitializer initializer = new TestDatabaseInitializer((username, password) ->
                provisionerCalls.incrementAndGet());

        assertThatThrownBy(() -> initializer.initialize(context(
                "jdbc:postgresql://localhost:5432/krishi_db")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("may connect only to")
                .hasNoCause();

        assertThat(provisionerCalls).hasValue(0);
    }

    @Test
    void rejectsConfiguredNonTestUrlBeforeProvisioning() {
        AtomicInteger provisionerCalls = new AtomicInteger();
        AtomicReference<String> effectiveDatasourceUrl = new AtomicReference<>();
        AtomicReference<String> openInViewSetting = new AtomicReference<>();
        String overriddenUrl = "jdbc:postgresql://localhost:5432/krishi_db";
        TestDatabaseInitializer initializer = new TestDatabaseInitializer((username, password) ->
                provisionerCalls.incrementAndGet());

        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withInitializer((ConfigurableApplicationContext context) -> {
                    effectiveDatasourceUrl.set(
                            context.getEnvironment().getProperty("spring.datasource.url"));
                    openInViewSetting.set(
                            context.getEnvironment().getProperty("spring.jpa.open-in-view"));
                    initializer.initialize(context);
                })
                .withPropertyValues("SPRING_TEST_DATASOURCE_URL=" + overriddenUrl)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(effectiveDatasourceUrl.get()).isEqualTo(overriddenUrl);
                    assertThat(openInViewSetting.get()).isEqualTo("false");
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("may connect only to");
                    assertThat(provisionerCalls).hasValue(0);
                });
    }

    @Test
    void missingDatasourceUrlFailsSafelyBeforeProvisioning() {
        AtomicInteger provisionerCalls = new AtomicInteger();
        TestDatabaseInitializer initializer = new TestDatabaseInitializer((username, password) ->
                provisionerCalls.incrementAndGet());

        assertThatThrownBy(() -> initializer.initialize(context(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.url is required")
                .hasNoCause();

        assertThat(provisionerCalls).hasValue(0);
    }

    @Test
    void malformedDatasourceUrlFailsSafelyBeforeProvisioning() {
        AtomicInteger provisionerCalls = new AtomicInteger();
        TestDatabaseInitializer initializer = new TestDatabaseInitializer((username, password) ->
                provisionerCalls.incrementAndGet());

        Throwable error = catchThrowable(() -> initializer.initialize(context("not-a-jdbc-url")));
        assertThat(error)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("may connect only to")
                .hasNoCause();
        assertThat(error.getMessage()).doesNotContain("not-a-jdbc-url");

        assertThat(provisionerCalls).hasValue(0);
    }

    @Test
    void provisioningFailureStopsInitializationWithSafeMessage() {
        String syntheticPassword = UUID.randomUUID().toString();
        TestDatabaseInitializer initializer = new TestDatabaseInitializer((username, password) -> {
            throw new SQLException("synthetic failure: " + syntheticPassword);
        });

        Throwable error = catchThrowable(() -> initializer.initialize(context(
                TestDatabaseInitializer.TEST_DATABASE_URL)));
        assertThat(error)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not initialize the dedicated local test database")
                .hasMessageContaining("SQLException")
                .hasNoCause();
        assertThat(error.getMessage()).doesNotContain(syntheticPassword);
    }

    private GenericApplicationContext context(String datasourceUrl) {
        GenericApplicationContext context = new GenericApplicationContext();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("SPRING_DATASOURCE_USERNAME", "test-user")
                .withProperty("SPRING_DATASOURCE_PASSWORD", UUID.randomUUID().toString());
        if (datasourceUrl != null) {
            environment.setProperty("spring.datasource.url", datasourceUrl);
        }
        context.setEnvironment(environment);
        return context;
    }
}

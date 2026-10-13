package com.newslit.backend.support;

import java.time.Duration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.oracle.OracleContainer;

public abstract class OracleDatabaseTestSupport {

    private static final String ORACLE_IMAGE = "gvenzl/oracle-free:23.26.3-slim-faststart";

    protected static final OracleContainer ORACLE = new OracleContainer(ORACLE_IMAGE)
            .withUsername("newslit")
            .withPassword("newslit-test")
            .withStartupTimeout(Duration.ofMinutes(3));

    static {
        ORACLE.start();
    }

    @DynamicPropertySource
    static void configureOracle(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ORACLE::getJdbcUrl);
        registry.add("spring.datasource.username", ORACLE::getUsername);
        registry.add("spring.datasource.password", ORACLE::getPassword);
        registry.add("spring.datasource.driver-class-name", ORACLE::getDriverClassName);
    }
}

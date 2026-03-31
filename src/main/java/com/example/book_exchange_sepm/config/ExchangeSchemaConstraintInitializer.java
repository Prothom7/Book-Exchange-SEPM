package com.example.book_exchange_sepm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@Order(120)
public class ExchangeSchemaConstraintInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ExchangeSchemaConstraintInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public ExchangeSchemaConstraintInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        String productName;
        try (Connection connection = dataSource.getConnection()) {
            productName = connection.getMetaData().getDatabaseProductName();
        }

        if (productName == null || !productName.toLowerCase().contains("postgres")) {
            return;
        }

        // Keep status constraint synchronized with ExchangeRequest.Status enum values.
        jdbcTemplate.execute("ALTER TABLE exchange_requests DROP CONSTRAINT IF EXISTS exchange_requests_status_check");
        jdbcTemplate.execute(
            "ALTER TABLE exchange_requests " +
            "ADD CONSTRAINT exchange_requests_status_check " +
            "CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED','COMPLETED'))"
        );

        log.info("Synchronized exchange_requests_status_check constraint with COMPLETED status support");
    }
}

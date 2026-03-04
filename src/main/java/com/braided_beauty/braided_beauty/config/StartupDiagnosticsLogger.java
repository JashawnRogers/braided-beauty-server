package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.models.ScheduleCalendar;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.ManagedType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StartupDiagnosticsLogger implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupDiagnosticsLogger.class);

    private final Environment environment;
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;

    @Override
    public void run(ApplicationArguments args) {
        String profiles = Arrays.toString(environment.getActiveProfiles());
        String dataSourceUrl = resolveDataSourceUrl(dataSource);
        ManagedType<ScheduleCalendar> managedType = entityManagerFactory.getMetamodel().managedType(ScheduleCalendar.class);
        Set<String> mappedAttributes = managedType.getAttributes().stream()
                .map(attribute -> attribute.getName())
                .collect(Collectors.toSet());

        String mappedFields = mappedAttributes.stream().sorted()
                .collect(Collectors.joining(","));

        log.info("Startup profiles: {}", profiles);
        log.info("Startup datasource URL: {}", dataSourceUrl);
        log.info("ScheduleCalendar mapped fields: {}", mappedFields);

        if (!mappedAttributes.contains("bookingOpenAt") || !mappedAttributes.contains("bookingCloseAt")) {
            throw new IllegalStateException("ScheduleCalendar must map bookingOpenAt/bookingCloseAt.");
        }
        if (mappedAttributes.contains("bookingOpenDate") || mappedAttributes.contains("bookingCloseDate")) {
            throw new IllegalStateException("Legacy ScheduleCalendar date mappings detected.");
        }
    }

    private static String resolveDataSourceUrl(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            return hikariDataSource.getJdbcUrl();
        }
        return dataSource.getClass().getName();
    }
}

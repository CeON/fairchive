package edu.harvard.iq.dataverse.persistence.config;

import org.flywaydb.core.Flyway;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;

@Startup
@Singleton
public class StartupFlywayMigrator {

    @Resource(lookup = "jdbc/VDCNetDS")
    private DataSource dataSource;

    @PostConstruct
    public void migrateDatabase() {

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .validateOnMigrate(false)
                .baselineOnMigrate(true)
                .load();

        long begin = System.currentTimeMillis();
        flyway.migrate();
        long end = System.currentTimeMillis();
        System.out.println("!================ DB migration done in: " + (end - begin) + " ms.");
    }
}


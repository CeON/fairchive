package edu.harvard.iq.dataverse.persistence.config;

import static java.lang.System.currentTimeMillis;
import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

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

        final long begin = currentTimeMillis();
        flyway.migrate();
        final long end = currentTimeMillis();
        logDuration(end - begin);
    }
    
	private static void logDuration(final long duration) {
    	getLogger(StartupFlywayMigrator.class).
    		info("DB version migration done in " + duration + " ms.");
    }
}


package edu.harvard.iq.dataverse.doi;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DoiBackgroundReservationInterval;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DoiProvider;
import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Timestamp;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;

import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.globalid.DOIDataCiteServiceBean;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 * Class designed to reserve doi's in the background.
 */
@Startup
@Singleton
@DependsOn("StartupFlywayMigrator")
public class DOIBackgroundReservationService {

    private static final Logger log = getLogger(DOIBackgroundReservationService.class);

    private SettingsServiceBean settings;
    private DatasetRepository datasetRepository;
    private DatasetService datasetService;
    private DOIDataCiteServiceBean doiDataCiteService;
    private IndexServiceBean indexServiceBean;

    private final Timer timer = new Timer();

    public DOIBackgroundReservationService() {}

    @Inject
    public DOIBackgroundReservationService(final SettingsServiceBean settings, 
                                           final DatasetRepository datasetRepository,
                                           final DatasetService datasetService, 
                                           final DOIDataCiteServiceBean doiDataCiteService,
                                           final IndexServiceBean indexServiceBean) {
        this.settings = settings;
        this.datasetRepository = datasetRepository;
        this.datasetService = datasetService;
        this.doiDataCiteService = doiDataCiteService;
        this.indexServiceBean = indexServiceBean;
    }

    @PostConstruct
    void startReservation() {
        reserveDoiPeriodically(this.timer);
    }

    @PreDestroy
    void preDestroy() {
        this.timer.cancel();
    }

    /**
     * Creates a timer which will reserve doi's in interval provided by 
     * 'DoiBackgroundReservationInterval'.
     */
    void reserveDoiPeriodically(final Timer timer) {
        final String provider = this.settings.getValueForKey(DoiProvider);
        if (provider.equals("DataCite")) {
        	final Long interval =  this.settings.getValueForKeyAsLong(DoiBackgroundReservationInterval);
        	if(interval != null) {
                timer.schedule(new ReservationTimerTask(), 0, interval);
                log.info("Activated DOI background reservation service..");
        	} else {
        		log.error("Could not read configuration value of 'DoiBackgroundReservationInterval' for 'DoiProvider' = 'DataCite'. DOI background reservation service inactive.");
        	}
        } else {
        	log.info("DOI background reservation service inactive.");
        }
    }

    void registerDataCiteIdentifier() {
    	// method has to be wrapped in try block, otherwise transaction rollback 
    	// could destroy timer setup.
    	try {
	        for (final Dataset nonReservedDataset : this.datasetRepository.
	        		findByNonRegisteredIdentifier()) {
	            final GlobalId globalId = generateGlobalIdFor(nonReservedDataset);
	            final Dataset refreshedDataset = this.datasetRepository.save(nonReservedDataset);
	            refreshedDataset.setIdentifier(globalId.getIdentifier());
	            try {
	            	this.doiDataCiteService.createIdentifier(refreshedDataset);
	            	refreshedDataset.setGlobalIdCreateTime(Timestamp.from(now()));
	                refreshedDataset.setIdentifierRegistered(true);
	                this.datasetRepository.save(refreshedDataset);
	                this.indexServiceBean.asyncIndexDataset(refreshedDataset, false);
	            } catch (final Exception e) {
	            	 log.info("Identifier could not be reserved.", e);
	            }
	        }
    	} catch (final Throwable e) {
    		log.error(e.getMessage(), e);
    	}
    }
    
    private GlobalId generateGlobalIdFor(final Dataset dataset) {
        GlobalId globalId = dataset.getGlobalId();
        int attempts = 0;
        
        while (this.doiDataCiteService.alreadyExists(globalId) && attempts < 10) {
            globalId = new GlobalId(dataset.getProtocol(), dataset.getAuthority(), 
            		this.datasetService.generateDatasetIdentifier(dataset));
            attempts++;
        }
        
        return globalId;
    }
    //--------------------------------------------------------------------------
    private class ReservationTimerTask extends TimerTask {
        @Override
        public void run() {
            registerDataCiteIdentifier();
        }
    }
}

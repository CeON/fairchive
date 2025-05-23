package edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class DataverseBannerRepository extends JpaRepository<Long, DataverseBanner> {

    public DataverseBannerRepository() {
        super(DataverseBanner.class);
    }

}

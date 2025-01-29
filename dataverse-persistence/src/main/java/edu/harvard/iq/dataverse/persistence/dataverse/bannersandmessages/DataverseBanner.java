package edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

@Entity
public class DataverseBanner {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Temporal(TIMESTAMP)
    private Date fromTime;

    @Column(nullable = false)
    @Temporal(TIMESTAMP)
    private Date toTime;

    private boolean active;

    @OneToMany(cascade = ALL, orphanRemoval = true, mappedBy = "dataverseBanner")
    private List<DataverseLocalizedBanner> dataverseLocalizedBanner = new ArrayList<>();

    @ManyToOne(fetch = LAZY)
    private Dataverse dataverse;

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Date getFromTime() {
        return this.fromTime;
    }

    public void setFromTime(final Date fromTime) {
        this.fromTime = fromTime;
    }

    public Date getToTime() {
        return this.toTime;
    }

    public void setToTime(final Date toTime) {
        this.toTime = toTime;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public List<DataverseLocalizedBanner> getDataverseLocalizedBanner() {
        return this.dataverseLocalizedBanner;
    }

    public void setDataverseLocalizedBanner(
            final List<DataverseLocalizedBanner> dataverseLocalizedBanner) {
        this.dataverseLocalizedBanner = dataverseLocalizedBanner;
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void setDataverse(final Dataverse dataverse) {
        this.dataverse = dataverse;
    }
}

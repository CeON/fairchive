package edu.harvard.iq.dataverse.persistence.harvest;

import static edu.harvard.iq.dataverse.persistence.harvest.HarvestStyle.DATAVERSE;
import static edu.harvard.iq.dataverse.persistence.harvest.HarvestType.OAI;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.function.Function.identity;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

@Table(indexes = {@Index(columnList = "dataverse_id")
        , @Index(columnList = "harvesttype")
        , @Index(columnList = "harveststyle")
        , @Index(columnList = "harvestingurl")})
@Entity
@NamedQueries({
        @NamedQuery(name = "HarvestingClient.findByNickname", 
        		   query = "SELECT hc FROM HarvestingClient hc WHERE LOWER(hc.name)=:nickName")
})
public class HarvestingClient implements Serializable, JpaEntity<Long> {
    private static final long serialVersionUID = 1L;
    
    public static final String SCHEDULE_PERIOD_DAILY = "daily";
    public static final String SCHEDULE_PERIOD_WEEKLY = "weekly";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "dataverse_id")
    private Dataverse dataverse;
    
    @OneToMany(mappedBy = "harvestedFrom", cascade = {MERGE, REMOVE}, orphanRemoval = true)
    private List<Dataset> harvestedDatasets;
    
    @NotBlank(message = "{user.enterNickname}")
    @Column(nullable = false, unique = true)
    @Size(max = 30, message = "{user.nicknameLength}")
    @Pattern.List({@Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", 
                            message = "{dataverse.nameIllegalCharacters}"),
    			   @Pattern(regexp = ".*\\D.*", 
    			            message = "{user.nicknameNotnumber}")})
    private String name;
    
    @Enumerated(EnumType.STRING)
    private HarvestType harvestType = OAI;
    
    @Enumerated(EnumType.STRING)
    private HarvestStyle harvestStyle = DATAVERSE; 
    
    // TODO: do we need "orphanRemoval=true"? -- L.A. 4.4
    // TODO: should it be @OrderBy("startTime")? -- L.A. 4.4
    @OneToMany(mappedBy = "harvestingClient", cascade = {REMOVE, MERGE, PERSIST})
    @OrderBy("id")
    private List<ClientHarvestRun> harvestHistory = new ArrayList<>();
    
    private String harvestingUrl;
    private String archiveUrl;
    private String harvestingSet;
    private String metadataPrefix;
    private boolean scheduled;
    private String schedulePeriod;
    private Integer scheduleHourOfDay;
    private Integer scheduleDayOfWeek;
    private boolean harvestingNow;
    private boolean deleted;

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void setDataverse(final Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public List<Dataset> getHarvestedDatasets() {
        return this.harvestedDatasets;
    }

    public void setHarvestedDatasets(final List<Dataset> datasets) {
        this.harvestedDatasets = datasets;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public HarvestType getHarvestType() {
        return harvestType;
    }

    public void setHarvestType(final HarvestType type) {
        this.harvestType = type;
    }

    public HarvestStyle getHarvestStyle() {
        return this.harvestStyle;
    }

    public void setHarvestStyle(final HarvestStyle style) {
        this.harvestStyle = style;
    }

    public String getHarvestingUrl() {
        return this.harvestingUrl;
    }

    public void setHarvestingUrl(final String url) {
    	this.harvestingUrl = trim(url);
    }

    public String getArchiveUrl() {
        return this.archiveUrl;
    }

    public void setArchiveUrl(final String url) {
        this.archiveUrl = url;
    }

    public String getHarvestingSet() {
        return this.harvestingSet;
    }

    public void setHarvestingSet(final String set) {
        this.harvestingSet = set;
    }

    public String getMetadataPrefix() {
        return this.metadataPrefix;
    }

    public void setMetadataPrefix(final String prefix) {
        this.metadataPrefix = prefix;
    }

    public List<ClientHarvestRun> getRunHistory() {
        return this.harvestHistory;
    }

    public void setRunHistory(final List<ClientHarvestRun> history) {
        this.harvestHistory = history != null ? history : new ArrayList<>();
    }
    
    public void addRun(final ClientHarvestRun run) {
    	this.harvestHistory.add(run);
    }

	public String getLastResult() {
		return getLastRun(run -> true, ClientHarvestRun::getResultLabel);
	}

    public ClientHarvestRun getLastRun() {
    	return getLastRun(run -> true, identity());
    }

	public ClientHarvestRun getLastSuccessfulRun() {
		return getLastRun(ClientHarvestRun::isSuccess, identity());
	}

    ClientHarvestRun getLastNonEmptyRun() {      
        return getLastRun(ClientHarvestRun::isNonEmpty, identity());
    }

    public Date getLastHarvestTime() {
    	return getLastRun(run -> true, ClientHarvestRun::getStartTime);
    }

    public Date getLastSuccessfulHarvestTime() {
    	return getLastRun(ClientHarvestRun::isSuccess, ClientHarvestRun::getStartTime);
    }

    public Date getLastNonEmptyHarvestTime() {
    	return getLastRun(ClientHarvestRun::isNonEmpty, ClientHarvestRun::getStartTime);
    }

    public Long getLastHarvestedDatasetCount() {
    	return getLastRun(ClientHarvestRun::isNonEmpty, ClientHarvestRun::getHarvestedDatasetCount);
    }

    public Long getLastFailedDatasetCount() {
    	return getLastRun(ClientHarvestRun::isNonEmpty, ClientHarvestRun::getFailedDatasetCount);
    }

    public Long getLastDeletedDatasetCount() {
    	return getLastRun(ClientHarvestRun::isNonEmpty, ClientHarvestRun::getDeletedDatasetCount);
    }

    public boolean isScheduled() {
        return this.scheduled;
    }

    public void setScheduled(final boolean scheduled) {
        this.scheduled = scheduled;
    }

    public String getSchedulePeriod() {
        return this.schedulePeriod;
    }

    public void setSchedulePeriod(final String period) {
        this.schedulePeriod = period;
    }

    public Integer getScheduleHourOfDay() {
        return this.scheduleHourOfDay;
    }

    public void setScheduleHourOfDay(final Integer scheduleHourOfDay) {
        this.scheduleHourOfDay = scheduleHourOfDay;
    }

    public Integer getScheduleDayOfWeek() {
        return this.scheduleDayOfWeek;
    }

    public void setScheduleDayOfWeek(final Integer scheduleDayOfWeek) {
        this.scheduleDayOfWeek = scheduleDayOfWeek;
    }

    public String getScheduleDescription() {
        if (isNoneEmpty(this.schedulePeriod)) {
            final Calendar cal = new GregorianCalendar();
            cal.set(HOUR_OF_DAY, this.scheduleHourOfDay);
            if (this.schedulePeriod.equals(SCHEDULE_PERIOD_WEEKLY)) {
                cal.set(DAY_OF_WEEK, this.scheduleDayOfWeek);
                return format("Weekly, ", " E h a ", cal.getTime());
            } else {
                return format("Daily, ", " h a ", cal.getTime());
            }
        } else {
        	return "Not Scheduled";
        }
    }

    public boolean isHarvestingNow() {
        return this.harvestingNow;
    }

    public void setHarvestingNow(final boolean harvestingNow) {
        this.harvestingNow = harvestingNow;
    }


    public boolean isDeleteInProgress() {
        return this.deleted;
    }

    public void setDeleteInProgress(boolean deleteInProgress) {
        this.deleted = deleteInProgress;
    }

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object other) {
    	return other instanceof HarvestingClient
    			? Objects.equals(this.id, ((HarvestingClient)other).id)
    			: false;
    }

    @Override
    public String toString() {
        return "HarvestingClient[name=" + this.name + " id=" + this.id + "]";
    }
    
	private <R> R getLastRun(final Predicate<ClientHarvestRun> condition, 
			final Function<ClientHarvestRun, R> mapper) {
		final ListIterator<ClientHarvestRun> it = this.harvestHistory.
				listIterator(this.harvestHistory.size());

		while (it.hasPrevious()) {
			final ClientHarvestRun run = it.previous();
			if (condition.test(run)) {
				return mapper.apply(run);
			}
		}
		return null;
	}
	
	private static String format(final String prefix, final String formatStr, 
			final Date date) {
		return prefix.concat(new SimpleDateFormat(formatStr).format(date));
	}
}

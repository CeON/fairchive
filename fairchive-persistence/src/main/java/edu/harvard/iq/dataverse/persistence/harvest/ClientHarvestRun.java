package edu.harvard.iq.dataverse.persistence.harvest;

import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

@Entity
public class ClientHarvestRun implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public enum RunResultType { SUCCESS, FAILURE, INPROGRESS }

    private static String RESULT_LABEL_SUCCESS = "SUCCESS";
    private static String RESULT_LABEL_FAILURE = "FAILED";
    private static String RESULT_LABEL_INPROGRESS = "INPROGRESS";
    private static String RESULT_DELETE_IN_PROGRESS = "DELETE IN PROGRESS";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    
    // Tese are the Dataset counts from that last harvest:
    // (TODO: do we need to differentiate between *created* (new), and *updated* 
    // harvested datasets? -- L.A. 4.4
    private Long harvestedDatasetCount = 0L;
    private Long failedDatasetCount = 0L;
    private Long deletedDatasetCount = 0L;

    @ManyToOne
    @JoinColumn(nullable = false)
    private HarvestingClient harvestingClient;
    
    private RunResultType harvestResult;
    
    @Temporal(value = TIMESTAMP)
    private Date startTime;
    
    @Temporal(value = TIMESTAMP)
    private Date finishTime;

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public HarvestingClient getHarvestingClient() {
        return harvestingClient;
    }

    public void setHarvestingClient(HarvestingClient harvestingClient) {
        this.harvestingClient = harvestingClient;
    }


    public RunResultType getResult() {
        return harvestResult;
    }

	public String getResultLabel() {
		if (this.harvestingClient != null && this.harvestingClient.isDeleteInProgress()) {
			return RESULT_DELETE_IN_PROGRESS;
		} else if (isSuccess()) {
			return RESULT_LABEL_SUCCESS;
		} else if (isFailed()) {
			return RESULT_LABEL_FAILURE;
		} else if (isInProgress()) {
			return RESULT_LABEL_INPROGRESS;
		} else {
			return null;
		}
	}

    public String getDetailedResultLabel() {
        if (isSuccess()) {
        	return new StringBuilder(RESULT_LABEL_SUCCESS).append("; ").
        			append(this.harvestedDatasetCount).append(" harvested, ").
        			append(this.deletedDatasetCount).append(" deleted, ").
        			append(this.failedDatasetCount).append(" failed.").toString();
        } else {
        	return getResultLabel();
        }
    }

    public void setResult(final RunResultType result) {
        this.harvestResult = result;
    }

    public boolean isSuccess() {
        return RunResultType.SUCCESS == this.harvestResult;
    }

    public void setSuccess() {
        harvestResult = RunResultType.SUCCESS;
    }

    public boolean isFailed() {
        return RunResultType.FAILURE == this.harvestResult;
    }

    public void setFailed() {
        this.harvestResult = RunResultType.FAILURE;
    }

    public boolean isInProgress() {
        return RunResultType.INPROGRESS == this.harvestResult ||
                (this.harvestResult == null && 
                	this.startTime != null && 
                	this.finishTime == null);
    }

    public void setInProgress() {
        this.harvestResult = RunResultType.INPROGRESS;
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public void setStartTime(final Date time) {
        this.startTime = time;
    }

    public Date getFinishTime() {
        return this.finishTime;
    }

    public void setFinishTime(final Date time) {
        this.finishTime = time;
    }

    public Long getHarvestedDatasetCount() {
        return this.harvestedDatasetCount;
    }

    public void setHarvestedDatasetCount(final Long count) {
        this.harvestedDatasetCount = count;
    }

    public Long getFailedDatasetCount() {
        return this.failedDatasetCount;
    }

    public void setFailedDatasetCount(Long count) {
        this.failedDatasetCount = count;
    }

    public Long getDeletedDatasetCount() {
        return this.deletedDatasetCount;
    }

    public void setDeletedDatasetCount(final Long count) {
        this.deletedDatasetCount = count;
    }
    
	boolean isNonEmpty() {
		return isSuccess() && 
				(this.harvestedDatasetCount > 0L || this.deletedDatasetCount > 0L);
	}

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object other) {
    	return other instanceof ClientHarvestRun
    			? Objects.equals(this.id, ((ClientHarvestRun)other).id)
    			: false;
    }

    @Override
    public String toString() {
        return "HarvestingClientRun[ id=" + id + " ]";
    }
}

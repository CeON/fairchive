package edu.harvard.iq.dataverse.persistence.harvest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.Test;

public class HervestingClientTest {

	@Test
	void equals_and_hashCOde() {
		
		HarvestingClient client1 = new HarvestingClient();
		HarvestingClient client2 = new HarvestingClient();
		
		assertThat(client1.hashCode()).isZero();
		assertThat(client2.hashCode()).isZero();
		
		assertThat(client1.equals(client2)).isTrue();
		assertThat(client1.equals(null)).isFalse();
		assertThat(client1.equals("")).isFalse();
		
		client1.setId(1L);
		assertThat(client1.hashCode()).isNotZero();
		assertThat(client1.equals(client2)).isFalse();
		
		client2.setId(1L);
		assertThat(client2.hashCode()).isNotZero();
		assertThat(client1.equals(client2)).isTrue();
	}
	
	@Test
	void harvestingUrl() {
		
		HarvestingClient client = new HarvestingClient();
		
		assertThat(client.getHarvestingUrl()).isNull();
		
		client.setHarvestingUrl(null);
		
		assertThat(client.getHarvestingUrl()).isNull();
		
		client.setHarvestingUrl("abc");
		
		assertThat(client.getHarvestingUrl()).isEqualTo("abc");
		
		client.setHarvestingUrl(" abc ");
		
		assertThat(client.getHarvestingUrl()).isEqualTo("abc");
	}
	
	
	@Test
	void getLastRun() {
		HarvestingClient client = new HarvestingClient();
		
		assertThat(client.getLastRun()).isNull();
		assertThat(client.getLastResult()).isNull();
		assertThat(client.getLastSuccessfulRun()).isNull();
		assertThat(client.getLastNonEmptyRun()).isNull();
		assertThat(client.getLastNonEmptyHarvestTime()).isNull();
		assertThat(client.getLastHarvestTime()).isNull();
		assertThat(client.getLastSuccessfulHarvestTime()).isNull();
		assertThat(client.getLastHarvestedDatasetCount()).isNull();
		assertThat(client.getLastFailedDatasetCount()).isNull();
		assertThat(client.getLastDeletedDatasetCount()).isNull();
		assertThat(client.getLastHarvestedDatasetCount()).isNull();
		
		ClientHarvestRun run1 = new ClientHarvestRun();
		run1.setResult(ClientHarvestRun.RunResultType.FAILURE);
		run1.setStartTime(new Date());
		client.addRun(run1);
		
		assertThat(client.getLastRun()).isSameAs(run1);
		assertThat(client.getLastResult()).isEqualTo("FAILED");
		assertThat(client.getLastSuccessfulRun()).isNull();
		assertThat(client.getLastNonEmptyRun()).isNull();
		assertThat(client.getLastNonEmptyHarvestTime()).isNull();
		assertThat(client.getLastHarvestTime()).isEqualTo(run1.getStartTime());
		assertThat(client.getLastSuccessfulHarvestTime()).isNull();
		assertThat(client.getLastHarvestedDatasetCount()).isNull();
		assertThat(client.getLastFailedDatasetCount()).isNull();
		assertThat(client.getLastDeletedDatasetCount()).isNull();
		assertThat(client.getLastHarvestedDatasetCount()).isNull();
		assertThat(client.getLastRun().getDetailedResultLabel()).isEqualTo("FAILED");
		
		ClientHarvestRun run2 = new ClientHarvestRun();
		run2.setResult(ClientHarvestRun.RunResultType.SUCCESS);
		run1.setStartTime(new Date());
		run2.setDeletedDatasetCount(1L);
		run2.setHarvestedDatasetCount(2L);
		client.addRun(run2);
		
		assertThat(client.getLastRun()).isSameAs(run2);
		assertThat(client.getLastResult()).isEqualTo("SUCCESS");
		assertThat(client.getLastSuccessfulRun()).isSameAs(run2);
		assertThat(client.getLastNonEmptyRun()).isSameAs(run2);
		assertThat(client.getLastNonEmptyHarvestTime()).isEqualTo(run2.getStartTime());
		assertThat(client.getLastHarvestTime()).isEqualTo(run2.getStartTime());
		assertThat(client.getLastSuccessfulHarvestTime()).isEqualTo(run2.getStartTime());
		assertThat(client.getLastFailedDatasetCount()).isZero();
		assertThat(client.getLastDeletedDatasetCount()).isEqualTo(1);
		assertThat(client.getLastHarvestedDatasetCount()).isEqualTo(2L);
		assertThat(client.getLastRun().getDetailedResultLabel()).
			isEqualTo("SUCCESS; 2 harvested, 1 deleted, 0 failed.");
	}
	
	@Test
	void getScheduleDescription_notScheduled() {
		HarvestingClient client = new HarvestingClient();
		
		assertThat(client.getScheduleDescription()).isEqualTo("Not Scheduled");
		
	}
	
	@Test
	void getScheduleDescription_daily() {
		HarvestingClient client = new HarvestingClient();	
		client.setSchedulePeriod(HarvestingClient.SCHEDULE_PERIOD_DAILY);
		
		client.setScheduleHourOfDay(0);
		
		assertThat(client.getScheduleDescription()).isEqualTo("Daily,  12 AM ");
		
		client.setScheduleHourOfDay(6);
		
		assertThat(client.getScheduleDescription()).isEqualTo("Daily,  6 AM ");
		
		client.setScheduleHourOfDay(20);
		
		assertThat(client.getScheduleDescription()).isEqualTo("Daily,  8 PM ");
		
		client.setScheduleHourOfDay(12);
		
		assertThat(client.getScheduleDescription()).isEqualTo("Daily,  12 PM ");
	}
	
	@Test
	void getScheduleDescription_weekly() {
		HarvestingClient client = new HarvestingClient();	
		client.setSchedulePeriod(HarvestingClient.SCHEDULE_PERIOD_WEEKLY);
		client.setScheduleHourOfDay(6);
		client.setScheduleDayOfWeek(0);
		
		assertThat(client.getScheduleDescription()).isEqualTo("Weekly,  Sat 6 AM ");
		
		client.setScheduleDayOfWeek(2);
		
		assertThat(client.getScheduleDescription()).isEqualTo("Weekly,  Mon 6 AM ");
		
		client.setScheduleDayOfWeek(6);
		
		assertThat(client.getScheduleDescription()).isEqualTo("Weekly,  Fri 6 AM ");
	}
}

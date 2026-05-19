package edu.harvard.iq.dataverse.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class SumStatCalculatorTest {
	
	@Test
	void calculateSummaryStatistics() {
		
		Number[] values = new Number[] {new Float(1.0), new Float(2.0), new Float(3.0)};
		
		double[] results = SumStatCalculator.calculateSummaryStatistics(values);
		
		assertThat(results).hasSize(8);
		assertThat(results[0]).isEqualTo(2.0);
		assertThat(results[1]).isEqualTo(2.0);
		assertThat(results[2]).isEqualTo(0.0);
		assertThat(results[3]).isEqualTo(3.0);
		assertThat(results[4]).isEqualTo(0.0);
		assertThat(results[5]).isEqualTo(1.0);
		assertThat(results[6]).isEqualTo(3.0);
		assertThat(results[7]).isEqualTo(1.0);
	}
}

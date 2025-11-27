package edu.harvard.iq.dataverse.metrics;

import org.junit.jupiter.api.Test;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.ChartSeries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChartCreatorTest {

    @Test
    public void verifyIfChartBarModelCorrectlyDistributesDatasets() {
        //given
        ChartCreator chartCreator = new ChartCreator();
        List<ChartMetrics> chartMetrics = generateSampleDatasetsMetrics();

        //when
        BarChartModel createdModel = chartCreator.createYearlyChart(chartMetrics, "publishedDatasets");

        //then
        assertEquals(100L, getMaximumYaxisHeight(createdModel));
        assertEquals(7, getYearValueFromModel(createdModel, 2018));
        assertEquals(78, getYearValueFromModel(createdModel, 2019));
        assertEquals(8, getYearValueFromModel(createdModel, 2020));
    }

    private Object getMaximumYaxisHeight(BarChartModel createdModel) {
        return createdModel.getAxis(AxisType.Y).getMax();
    }

    private int getYearValueFromModel(BarChartModel createdModel, int year) {
        ChartSeries chartSeries = createdModel.getSeries().get(0);
        Number number = chartSeries.getData().get(year);
        return number.intValue();
    }

    private List<ChartMetrics> generateSampleDatasetsMetrics() {
        return Arrays.asList(
                new ChartMetrics(2018, 4, 7L),
                new ChartMetrics(2019, 1, 78L),
                new ChartMetrics(2020, 12, 8L)
        );
    }

    @Test
    public void verifyRoundMaxChartValue() {
        // given

        List<Map<String, Long>> values = Arrays.asList(
            createValuepair(1L, 5L),
            createValuepair(12L, 25L),
            createValuepair(123L, 250L),
            createValuepair(1234L, 2500L),
            createValuepair(12345L, 25000L),
            createValuepair(21L, 25L),
            createValuepair(321L, 500L),
            createValuepair(4321L, 5000L),
            createValuepair(54321L, 100000L),
            createValuepair(2999L, 5000L),
            createValuepair(499999L, 1000000L),
            createValuepair(9999L, 25000L)
        );

        // execute & assert

        for (Map<String, Long> value : values) {
            ChartCreator chartCreator = new ChartCreator();
            List<ChartMetrics> chartMetrics = Arrays.asList(
                new ChartMetrics(2025, 11, value.get("given"))
            );
            BarChartModel createdModel = chartCreator.createYearlyChart(chartMetrics, "publishedDatasets");

            assertEquals(value.get("expected"), getMaximumYaxisHeight(createdModel));
        }

    }

    private Map<String, Long> createValuepair(long given, long expected) {
        Map<String, Long> dict = new HashMap<>(); 
        dict.put("given", given);
        dict.put("expected", expected);
        return dict;
    }
}

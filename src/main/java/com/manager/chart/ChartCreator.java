package com.manager.chart;

import com.manager.data.dto.Currency;
import com.manager.data.graph.DataPoint;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class ChartCreator {

  public void createChart(List<DataPoint> dataPoints, String outputFile, String period) {
    TimeSeries series = new TimeSeries("Values");
    for (DataPoint dataPoint : dataPoints) {
      series.add(new Day(dataPoint.getDate()), dataPoint.getSaleRate());
    }

    TimeSeriesCollection dataset = new TimeSeriesCollection();
    dataset.addSeries(series);

    JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Exchange rate of " + dataPoints.getFirst().getCurrency() + " for " + period,
            "Dates",           // X-axis Label
            "Exchange Rate",          // Y-axis Label
            dataset,          // Dataset
            false,            // Legend
            true,             // Tooltips
            false             // URLs
    );

    try {
      ChartUtils.saveChartAsPNG(new File(outputFile), chart, 800, 600);
    } catch (IOException e) {
      throw new RuntimeException("Error while saving chart", e);
    }
  }

  public void parsePeriod(List<Currency> currencyList, String period) {
    var currencies = currencyList.stream()
            .sorted(Comparator.comparing(Currency::date)).toList();

    var dataPoints = parseData(currencies);
    var file = "chart_" + currencyList.getFirst().currency() + ".png";
    createChart(dataPoints, file, period);

    log.info("Chart saved as {}", file);
  }

  public List<DataPoint> parseData(List<Currency> exchangeRates) {
    List<DataPoint> dataPoints = new ArrayList<>();

    for (Currency item : exchangeRates) {
      var dataPoint = DataPoint.builder()
              .date(Date.from(item.date().atStartOfDay(ZoneId.systemDefault()).toInstant()))
              .currency(item.currency())
              .saleRate(item.saleRate())
              .build();

      dataPoints.add(dataPoint);
    }
    return dataPoints;
  }

}

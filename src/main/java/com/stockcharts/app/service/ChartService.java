package com.stockcharts.app.service;

import com.stockcharts.app.model.ChartRequest;
import org.springframework.ai.tool.annotation.Tool;
import com.stockcharts.app.model.LineData;
import com.stockcharts.app.model.OhlcData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Day;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.time.ohlc.OHLCSeries;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class ChartService {

    static {
        // Ensure charts render in environments without a display
        System.setProperty("java.awt.headless", "true");
    }

    @Tool(description = "Generate a stock chart for a given symbol with specified chart type (candlestick, line, ohlc)")
    public String generateChart(String symbol, String chartType, String period, String startDate, String endDate) {
        try {
            ChartRequest request = new ChartRequest();
            // Use reflection to set fields since we don't have setters
            java.lang.reflect.Field symbolField = ChartRequest.class.getDeclaredField("symbol");
            symbolField.setAccessible(true);
            symbolField.set(request, symbol);
            
            java.lang.reflect.Field chartTypeField = ChartRequest.class.getDeclaredField("chartType");
            chartTypeField.setAccessible(true);
            chartTypeField.set(request, chartType != null ? chartType : "candlestick");
            
            java.lang.reflect.Field periodField = ChartRequest.class.getDeclaredField("period");
            periodField.setAccessible(true);
            periodField.set(request, period != null ? period : "1D");
            
            java.lang.reflect.Field startDateField = ChartRequest.class.getDeclaredField("startDate");
            startDateField.setAccessible(true);
            startDateField.set(request, startDate);
            
            java.lang.reflect.Field endDateField = ChartRequest.class.getDeclaredField("endDate");
            endDateField.setAccessible(true);
            endDateField.set(request, endDate);
            
            byte[] chartData = generateChartBytes(request);
            String base64Chart = Base64.getEncoder().encodeToString(chartData);
            
            return "Chart generated successfully for " + symbol + ". Chart data: data:image/png;base64," + base64Chart;
        } catch (Exception e) {
            return "Error generating chart: " + e.getMessage();
        }
    }

    public byte[] generateChartBytes(ChartRequest request) throws IOException {
        JFreeChart chart = createOHLCChart(request);
        
        if (request.getLines() != null && !request.getLines().isEmpty()) {
            addCustomLines(chart, request);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsJPEG(baos, chart, request.getWidth(), request.getHeight());
        return baos.toByteArray();
    }

    private JFreeChart createOHLCChart(ChartRequest request) {
        OHLCSeries series = new OHLCSeries("Stock Data");
        
        double minLow = Double.POSITIVE_INFINITY;
        double maxHigh = Double.NEGATIVE_INFINITY;
        for (OhlcData data : request.getOhlcData()) {
            Date date = Date.from(data.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            series.add(new Day(date), data.getOpen(), data.getHigh(), data.getLow(), data.getClose());
            if (data.getLow() < minLow) minLow = data.getLow();
            if (data.getHigh() > maxHigh) maxHigh = data.getHigh();
        }
        
        OHLCSeriesCollection dataset = new OHLCSeriesCollection();
        dataset.addSeries(series);
        
        JFreeChart chart = ChartFactory.createCandlestickChart(
                null,
                "Date",
                "Price",
                dataset,
                false
        );
        
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);
        chart.setPadding(new org.jfree.chart.ui.RectangleInsets(5, 5, 5, 5));
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setOutlineVisible(true);
        
        // Configure date axis
        plot.getDomainAxis().setVisible(true);
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        
        // Configure price axis
        plot.getRangeAxis().setVisible(true);
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        
        // Format number axis to show currency
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setNumberFormatOverride(NumberFormat.getCurrencyInstance());
        // Auto-adjust range based on dataset min/max (with small headroom)
        if (minLow != Double.POSITIVE_INFINITY && maxHigh != Double.NEGATIVE_INFINITY) {
            double range = Math.max(1e-9, maxHigh - minLow);
            double pad = range * 0.05; // 5% headroom
            rangeAxis.setAutoRange(false);
            rangeAxis.setLowerBound(minLow - pad);
            rangeAxis.setUpperBound(maxHigh + pad);
        } else {
            // Fallback to auto if no data
            rangeAxis.setAutoRange(true);
        }
        
        return chart;
    }

    private void addCustomLines(JFreeChart chart, ChartRequest request) {
        XYPlot plot = (XYPlot) chart.getPlot();
        
        for (LineData line : request.getLines()) {
            double startDateMillis = line.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            double endDateMillis = line.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            XYLineAnnotation annotation = new XYLineAnnotation(
                    startDateMillis,
                    line.getStartValue(),
                    endDateMillis,
                    line.getEndValue(),
                    new BasicStroke(line.getStrokeWidth()),
                    line.getAwtColor()
            );
            
            plot.addAnnotation(annotation);
        }
    }
}

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
    
    private final PolygonService polygonService;
    
    public ChartService(PolygonService polygonService) {
        this.polygonService = polygonService;
    }

    static {
        // Ensure charts render in environments without a display
        System.setProperty("java.awt.headless", "true");
    }

    @Tool(description = "Generate a stock chart for a given symbol or ratio (e.g., AAPL or AAPL/SPY) with specified chart type (candlestick, line, ohlc). To draw a line, provide lineStartDate, lineEndDate, lineStartValue, lineEndValue.")
    public String generateChart(String symbol, String chartType, String period, String startDate, String endDate, String lineStartDate, String lineEndDate, Double lineStartValue, Double lineEndValue) {
        try {
            ChartRequest request = new ChartRequest();
            request.setSymbol(symbol);
            request.setChartType(chartType != null ? chartType : "candlestick");
            request.setPeriod(period != null ? period : "1D");
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setTitle(symbol + " Stock Chart");
            
            // Check if this is a ratio (contains "/")
            java.util.List<OhlcData> stockData;
            if (symbol.contains("/")) {
                stockData = calculateRatioData(symbol, startDate, endDate);
            } else {
                // Get stock data from PolygonService
                stockData = polygonService.getAggregates(
                    symbol, "1", "day", 
                    startDate != null ? startDate : "2025-08-01", 
                    endDate != null ? endDate : "2025-09-05", 
                    true, "asc", 100);
            }
            request.setOhlcData(stockData);
            
            // Add custom line if coordinates provided
            if (lineStartDate != null && lineEndDate != null && lineStartValue != null && lineEndValue != null) {
                java.util.List<LineData> lines = new java.util.ArrayList<>();
                LineData customLine = new LineData(
                    java.time.LocalDate.parse(lineStartDate),
                    java.time.LocalDate.parse(lineEndDate),
                    lineStartValue,
                    lineEndValue
                );
                customLine.setColor("#FF0000"); // Red line
                customLine.setStrokeWidth(4.0f); // Thicker line
                lines.add(customLine);
                request.setLines(lines);
                System.out.println("DEBUG: Added line from " + lineStartDate + " ($" + lineStartValue + ") to " + lineEndDate + " ($" + lineEndValue + ")");
            }
            
            byte[] chartData = generateChartBytes(request);
            
            // Save chart to file
            String fileName = symbol.replace("/", "_") + "_" + chartType + "_chart.png";
            String filePath = fileName;
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get(filePath), chartData);
                return "Chart generated successfully for " + symbol + ". Chart saved to: " + filePath;
            } catch (Exception e) {
                return "Error saving chart to file: " + e.getMessage();
            }
        } catch (IOException | InterruptedException e) {
            return "Error fetching stock data: " + e.getMessage();
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
        ChartUtils.writeChartAsPNG(baos, chart, request.getWidth(), request.getHeight());
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
            // Convert dates to Day objects like the OHLC data, then get the serial number
            Date startDate = Date.from(line.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endDate = Date.from(line.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Day startDay = new Day(startDate);
            Day endDay = new Day(endDate);
            
            XYLineAnnotation annotation = new XYLineAnnotation(
                    startDay.getMiddleMillisecond(),
                    line.getStartValue(),
                    endDay.getMiddleMillisecond(),
                    line.getEndValue(),
                    new BasicStroke(line.getStrokeWidth()),
                    line.getAwtColor()
            );
            
            System.out.println("DEBUG: Adding line annotation from " + startDay.getMiddleMillisecond() + "," + line.getStartValue() + " to " + endDay.getMiddleMillisecond() + "," + line.getEndValue());
            plot.addAnnotation(annotation);
        }
    }
    
    private java.util.List<OhlcData> calculateRatioData(String ratioSymbol, String startDate, String endDate) throws IOException, InterruptedException {
        String[] symbols = ratioSymbol.split("/");
        if (symbols.length != 2) {
            throw new IllegalArgumentException("Invalid ratio format. Use SYMBOL1/SYMBOL2");
        }
        
        String numeratorSymbol = symbols[0].trim();
        String denominatorSymbol = symbols[1].trim();
        
        // Get data for both symbols
        java.util.List<OhlcData> numeratorData = polygonService.getAggregates(
            numeratorSymbol, "1", "day", 
            startDate != null ? startDate : "2025-08-01", 
            endDate != null ? endDate : "2025-09-05", 
            true, "asc", 100);
            
        java.util.List<OhlcData> denominatorData = polygonService.getAggregates(
            denominatorSymbol, "1", "day", 
            startDate != null ? startDate : "2025-08-01", 
            endDate != null ? endDate : "2025-09-05", 
            true, "asc", 100);
            
        // Create a map for denominator data for quick lookup
        java.util.Map<java.time.LocalDate, OhlcData> denominatorMap = new java.util.HashMap<>();
        for (OhlcData data : denominatorData) {
            denominatorMap.put(data.getDate(), data);
        }
        
        // Calculate ratio data
        java.util.List<OhlcData> ratioData = new java.util.ArrayList<>();
        for (OhlcData numData : numeratorData) {
            OhlcData denomData = denominatorMap.get(numData.getDate());
            if (denomData != null && denomData.getClose() != 0 && denomData.getOpen() != 0 && 
                denomData.getHigh() != 0 && denomData.getLow() != 0) {
                
                double ratioOpen = numData.getOpen() / denomData.getOpen();
                double ratioHigh = numData.getHigh() / denomData.getHigh();
                double ratioLow = numData.getLow() / denomData.getLow();
                double ratioClose = numData.getClose() / denomData.getClose();
                
                OhlcData ratioOhlc = new OhlcData(numData.getDate(), ratioOpen, ratioHigh, ratioLow, ratioClose);
                ratioData.add(ratioOhlc);
            }
        }
        
        return ratioData;
    }
}

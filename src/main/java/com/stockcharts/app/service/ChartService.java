package com.stockcharts.app.service;

import com.stockcharts.app.model.ChartRequest;
import com.stockcharts.app.model.IndicatorSpec;
import org.springframework.ai.tool.annotation.Tool;
import com.stockcharts.app.model.LineData;
import com.stockcharts.app.model.OhlcData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.time.ohlc.OHLCSeries;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class ChartService {
    
    private final PolygonService polygonService;
    private final IndicatorService indicatorService;
    
    public ChartService(PolygonService polygonService, IndicatorService indicatorService) {
        this.polygonService = polygonService;
        this.indicatorService = indicatorService;
    }

    static {
        // Ensure charts render in environments without a display
        System.setProperty("java.awt.headless", "true");
    }

    @Tool(description = "Generate a comprehensive stock chart for a symbol or ratio with technical indicators. " +
          "Parameters: symbol (e.g., 'AAPL' or 'AAPL/SPY'), chartType ('candlestick'|'line'|'ohlc'), period ('1D'), " +
          "startDate ('2025-08-01'), endDate ('2025-09-05'), " +
          "indicators (comma-separated list: 'SMA:20:overlay,RSI:14:panel,MACD:12:panel'), " +
          "lineStartDate, lineEndDate, lineStartValue, lineEndValue (for custom trend lines), " +
          "fibonacciHigh, fibonacciLow (for Fibonacci retracements/extensions - use recent swing high/low values). " +
          "Indicators format: 'TYPE:PERIOD:DISPLAY' where DISPLAY is 'overlay' (same pane) or 'panel' (separate pane). " +
          "Fibonacci levels: automatically draws 23.6%, 38.2%, 50%, 61.8%, 76.4% retracements and 127.2%, 161.8% extensions. " +
          "Returns file path to generated PNG chart.")
    public String generateChart(String symbol, String chartType, String period, String startDate, String endDate, 
                               String indicators, String lineStartDate, String lineEndDate, 
                               Double lineStartValue, Double lineEndValue, Double fibonacciHigh, Double fibonacciLow) {
        try {
            ChartRequest request = new ChartRequest();
            request.setSymbol(symbol);
            request.setChartType(chartType != null ? chartType : "candlestick");
            request.setPeriod(period != null ? period : "1D");
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setTitle(symbol + " Stock Chart");

            // Parse indicators, if provided
            if (indicators != null && !indicators.trim().isEmpty()) {
                request.setIndicators(parseIndicators(indicators));
            }
            
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
            
            // Add custom lines if coordinates provided
            java.util.List<LineData> lines = new java.util.ArrayList<>();
            
            // Handle horizontal line from single point
            if (lineStartValue != null && lineEndValue == null) {
                LineData horizontalLine = new LineData(
                    java.time.LocalDate.parse(startDate),
                    java.time.LocalDate.parse(endDate),
                    lineStartValue,
                    lineStartValue
                );
                horizontalLine.setColor("#FF0000"); // Red line
                horizontalLine.setStrokeWidth(2.0f);
                lines.add(horizontalLine);
                System.out.println("DEBUG: Added horizontal line at $" + lineStartValue);
            }
            
            // Handle two-point line (extend to chart boundaries)
            if (lineStartValue != null && lineEndValue != null) {
                // Calculate slope and extend line to chart boundaries
                java.time.LocalDate chartStart = java.time.LocalDate.parse(startDate);
                java.time.LocalDate chartEnd = java.time.LocalDate.parse(endDate);
                
                java.time.LocalDate pointStart = lineStartDate != null ? java.time.LocalDate.parse(lineStartDate) : chartStart;
                java.time.LocalDate pointEnd = lineEndDate != null ? java.time.LocalDate.parse(lineEndDate) : chartEnd;
                
                // Calculate slope
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(pointStart, pointEnd);
                double slope = daysBetween != 0 ? (lineEndValue - lineStartValue) / daysBetween : 0;
                
                // Extend to chart boundaries
                long daysFromChartStart = java.time.temporal.ChronoUnit.DAYS.between(chartStart, pointStart);
                long daysToChartEnd = java.time.temporal.ChronoUnit.DAYS.between(pointEnd, chartEnd);
                
                double extendedStartValue = lineStartValue - (slope * daysFromChartStart);
                double extendedEndValue = lineEndValue + (slope * daysToChartEnd);
                
                LineData extendedLine = new LineData(
                    chartStart,
                    chartEnd,
                    extendedStartValue,
                    extendedEndValue
                );
                extendedLine.setColor("#FF6B35"); // Orange line
                extendedLine.setStrokeWidth(2.0f);
                lines.add(extendedLine);
                System.out.println("DEBUG: Added extended line from " + chartStart + " ($" + extendedStartValue + ") to " + chartEnd + " ($" + extendedEndValue + ")");
            }
            
            // Add Fibonacci retracements and extensions if high/low provided
            if (fibonacciHigh != null && fibonacciLow != null) {
                java.time.LocalDate chartStart = java.time.LocalDate.parse(startDate);
                java.time.LocalDate chartEnd = java.time.LocalDate.parse(endDate);
                lines.addAll(generateFibonacciLines(chartStart, chartEnd, fibonacciHigh, fibonacciLow));
            }
            
            if (!lines.isEmpty()) {
                request.setLines(lines);
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
            e.printStackTrace();
            return "Error generating chart: " + e.getMessage();
        }
    }

    public byte[] generateChartBytes(ChartRequest request) throws IOException {
        // Base price plot
        JFreeChart chart = createOHLCChart(request);

        // Apply indicators (overlays/panels)
        if (request.getIndicators() != null && !request.getIndicators().isEmpty()) {
            chart = applyIndicators(chart, request);
        }
        
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
        XYPlot plot;
        if (chart.getPlot() instanceof CombinedDomainXYPlot) {
            CombinedDomainXYPlot combined = (CombinedDomainXYPlot) chart.getPlot();
            // Add to the first subplot (price plot)
            plot = (XYPlot) combined.getSubplots().get(0);
        } else {
            plot = (XYPlot) chart.getPlot();
        }
        
        // Color palette for lines
        String[] lineColors = {"#FF6B35", "#F7931E", "#FFD23F", "#06FFA5", "#118AB2", "#073B4C", "#DD1C77", "#9D4EDD"};
        
        for (int i = 0; i < request.getLines().size(); i++) {
            LineData line = request.getLines().get(i);
            
            // Convert dates to Day objects like the OHLC data, then get the serial number
            Date startDate = Date.from(line.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endDate = Date.from(line.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Day startDay = new Day(startDate);
            Day endDay = new Day(endDate);
            
            // Use different colors for each line
            Color lineColor = line.getAwtColor();
            if (lineColor.equals(Color.decode("#FF0000"))) { // If default red, use color palette
                lineColor = Color.decode(lineColors[i % lineColors.length]);
            }
            
            XYLineAnnotation annotation = new XYLineAnnotation(
                    startDay.getMiddleMillisecond(),
                    line.getStartValue(),
                    endDay.getMiddleMillisecond(),
                    line.getEndValue(),
                    new BasicStroke(line.getStrokeWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
                    lineColor
            );
            
            System.out.println("DEBUG: Adding line annotation from " + startDay.getMiddleMillisecond() + "," + line.getStartValue() + " to " + endDay.getMiddleMillisecond() + "," + line.getEndValue() + " with color " + lineColor);
            plot.addAnnotation(annotation);
        }
    }
    
    private java.util.List<IndicatorSpec> parseIndicators(String indicatorsArg) {
        java.util.List<IndicatorSpec> list = new java.util.ArrayList<>();
        for (String part : indicatorsArg.split(",")) {
            if (part == null || part.trim().isEmpty()) continue;
            String[] tokens = part.trim().split(":");
            String type = tokens[0].trim().toUpperCase();
            int period = 0;
            IndicatorSpec.Display display = IndicatorSpec.Display.OVERLAY;
            if (tokens.length > 1) {
                try { period = Integer.parseInt(tokens[1].trim()); } catch (Exception ignored) {}
            }
            if (tokens.length > 2) {
                String disp = tokens[2].trim().toLowerCase();
                display = "panel".equals(disp) ? IndicatorSpec.Display.PANEL : IndicatorSpec.Display.OVERLAY;
            }
            // Reasonable defaults if period not parsed
            if (period <= 0) {
                period = switch (type) {
                    case "RSI" -> 14;
                    case "SMA", "EMA", "DPO", "DETRENDED" -> 20;
                    case "MACD" -> 12; // main fast setting; MACD handled specially if needed later
                    default -> 14;
                };
            }
            list.add(new IndicatorSpec(type, period, display));
        }
        return list;
    }

    private JFreeChart applyIndicators(JFreeChart baseChart, ChartRequest request) {
        XYPlot pricePlot = (XYPlot) baseChart.getPlot();
        DateAxis sharedDomainAxis = (DateAxis) pricePlot.getDomainAxis();

        // Overlay indicators on price plot
        for (IndicatorSpec spec : request.getIndicators()) {
            if (spec.getDisplay() == IndicatorSpec.Display.OVERLAY) {
                TimeSeriesCollection tsc = new TimeSeriesCollection();
                TimeSeries ts = new TimeSeries(spec.getType());
                switch (spec.getType()) {
                    case "SMA" -> {
                        for (IndicatorService.IndicatorValue v : indicatorService.sma(request.getOhlcData(), spec.getPeriod())) {
                            java.util.Date d = java.util.Date.from(v.getDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                            ts.add(new Day(d), v.getValue());
                        }
                    }
                    case "EMA" -> {
                        for (IndicatorService.IndicatorValue v : indicatorService.ema(request.getOhlcData(), spec.getPeriod())) {
                            java.util.Date d = java.util.Date.from(v.getDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                            ts.add(new Day(d), v.getValue());
                        }
                    }
                    default -> { /* skip non-overlay types here */ }
                }
                if (ts.getItemCount() > 0) {
                    tsc.addSeries(ts);
                    int datasetIndex = pricePlot.getDatasetCount();
                    pricePlot.setDataset(datasetIndex, tsc);
                    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
                    
                    // Color palette for overlay indicators
                    java.awt.Color[] overlayColors = {
                        new java.awt.Color(0x1f, 0x77, 0xb4), // Blue
                        new java.awt.Color(0xff, 0x7f, 0x0e), // Orange
                        new java.awt.Color(0x2c, 0xa0, 0x2c), // Green
                        new java.awt.Color(0xd6, 0x27, 0x28), // Red
                        new java.awt.Color(0x94, 0x67, 0xbd), // Purple
                        new java.awt.Color(0x8c, 0x56, 0x4b), // Brown
                    };
                    
                    renderer.setSeriesPaint(0, overlayColors[(datasetIndex - 1) % overlayColors.length]);
                    renderer.setDefaultStroke(new java.awt.BasicStroke(2.0f));
                    pricePlot.setRenderer(datasetIndex, renderer);
                }
            }
        }

        // Build panel indicators
        java.util.List<XYPlot> panelPlots = new java.util.ArrayList<>();
        for (IndicatorSpec spec : request.getIndicators()) {
            if (spec.getDisplay() == IndicatorSpec.Display.PANEL) {
                TimeSeries ts = new TimeSeries(spec.getType());
                String yLabel = spec.getType();
                NumberAxis axis = new NumberAxis(yLabel);
                switch (spec.getType()) {
                    case "RSI" -> {
                        for (IndicatorService.IndicatorValue v : indicatorService.rsi(request.getOhlcData(), spec.getPeriod())) {
                            java.util.Date d = java.util.Date.from(v.getDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                            ts.add(new Day(d), v.getValue());
                        }
                        axis.setRange(0, 100);
                    }
                    case "DPO", "DETRENDED" -> {
                        for (IndicatorService.IndicatorValue v : indicatorService.detrendedPriceOscillator(request.getOhlcData(), spec.getPeriod())) {
                            java.util.Date d = java.util.Date.from(v.getDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                            ts.add(new Day(d), v.getValue());
                        }
                    }
                    // MACD could be added here later
                    default -> { /* ignore unsupported panels for now */ }
                }

                if (ts.getItemCount() > 0) {
                    TimeSeriesCollection tsc = new TimeSeriesCollection();
                    tsc.addSeries(ts);
                    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
                    
                    // Color palette for panel indicators
                    java.awt.Color[] panelColors = {
                        new java.awt.Color(0xe3, 0x77, 0xc2), // Pink
                        new java.awt.Color(0x17, 0xbe, 0xcf), // Cyan
                        new java.awt.Color(0x7f, 0x7f, 0x7f), // Gray
                        new java.awt.Color(0xbc, 0xbd, 0x22), // Olive
                        new java.awt.Color(0xff, 0x97, 0x96), // Light Red
                        new java.awt.Color(0x9e, 0xda, 0xe5), // Light Blue
                    };
                    
                    renderer.setSeriesPaint(0, panelColors[panelPlots.size() % panelColors.length]);
                    renderer.setDefaultStroke(new java.awt.BasicStroke(2.0f));
                    
                    XYPlot subPlot = new XYPlot(tsc, null, axis, renderer);
                    subPlot.setBackgroundPaint(java.awt.Color.WHITE);
                    subPlot.setDomainGridlinePaint(java.awt.Color.LIGHT_GRAY);
                    subPlot.setRangeGridlinePaint(java.awt.Color.LIGHT_GRAY);
                    panelPlots.add(subPlot);
                }
            }
        }

        if (panelPlots.isEmpty()) {
            return baseChart; // no panels, keep single-plot chart
        }

        // Build a combined plot with shared domain axis
        CombinedDomainXYPlot combined = new CombinedDomainXYPlot(sharedDomainAxis);
        combined.setGap(6.0);
        // Add price plot with higher weight
        combined.add(pricePlot, 3);
        // Add indicator panels
        for (XYPlot p : panelPlots) combined.add(p, 1);

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, combined, false);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setPadding(new org.jfree.chart.ui.RectangleInsets(5, 5, 5, 5));
        return chart;
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
    
    private java.util.List<LineData> generateFibonacciLines(java.time.LocalDate chartStart, java.time.LocalDate chartEnd, 
                                                           double high, double low) {
        java.util.List<LineData> fibLines = new java.util.ArrayList<>();
        double range = high - low;
        
        // Fibonacci retracement levels
        double[] retracementLevels = {0.236, 0.382, 0.5, 0.618, 0.764};
        String[] retracementColors = {"#FFD700", "#FF8C00", "#FF6347", "#9370DB", "#20B2AA"};
        
        for (int i = 0; i < retracementLevels.length; i++) {
            double level = high - (range * retracementLevels[i]);
            LineData fibLine = new LineData(chartStart, chartEnd, level, level);
            fibLine.setColor(retracementColors[i]);
            fibLine.setStrokeWidth(1.5f);
            fibLines.add(fibLine);
            System.out.println("DEBUG: Added Fibonacci retracement " + (retracementLevels[i] * 100) + "% at $" + level);
        }
        
        // Fibonacci extension levels
        double[] extensionLevels = {1.272, 1.618};
        String[] extensionColors = {"#FF1493", "#8A2BE2"};
        
        for (int i = 0; i < extensionLevels.length; i++) {
            double extensionDown = low - (range * (extensionLevels[i] - 1));
            double extensionUp = high + (range * (extensionLevels[i] - 1));
            
            // Extension below
            LineData fibExtensionDown = new LineData(chartStart, chartEnd, extensionDown, extensionDown);
            fibExtensionDown.setColor(extensionColors[i]);
            fibExtensionDown.setStrokeWidth(1.5f);
            fibLines.add(fibExtensionDown);
            
            // Extension above
            LineData fibExtensionUp = new LineData(chartStart, chartEnd, extensionUp, extensionUp);
            fibExtensionUp.setColor(extensionColors[i]);
            fibExtensionUp.setStrokeWidth(1.5f);
            fibLines.add(fibExtensionUp);
            
            System.out.println("DEBUG: Added Fibonacci extension " + (extensionLevels[i] * 100) + "% at $" + extensionDown + " and $" + extensionUp);
        }
        
        // Add 0% (high) and 100% (low) reference lines
        LineData highLine = new LineData(chartStart, chartEnd, high, high);
        highLine.setColor("#00FF00"); // Green for high
        highLine.setStrokeWidth(2.0f);
        fibLines.add(highLine);
        
        LineData lowLine = new LineData(chartStart, chartEnd, low, low);
        lowLine.setColor("#FF0000"); // Red for low
        lowLine.setStrokeWidth(2.0f);
        fibLines.add(lowLine);
        
        System.out.println("DEBUG: Added Fibonacci reference lines - High: $" + high + ", Low: $" + low);
        
        return fibLines;
    }
}

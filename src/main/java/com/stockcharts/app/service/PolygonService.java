package com.stockcharts.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockcharts.app.config.Config;
import com.stockcharts.app.model.OhlcData;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PolygonService {
    
    private static final String BASE_URL = "https://api.polygon.io";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public PolygonService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }
    
    public List<OhlcData> getAggregates(String ticker, String multiplier, String timespan, 
                                       String from, String to, boolean adjusted, String sort, int limit) throws IOException, InterruptedException {
        
        String url = String.format("%s/v2/aggs/ticker/%s/range/%s/%s/%s/%s", 
            BASE_URL, 
            URLEncoder.encode(ticker.toUpperCase(), StandardCharsets.UTF_8),
            URLEncoder.encode(multiplier, StandardCharsets.UTF_8),
            URLEncoder.encode(timespan, StandardCharsets.UTF_8),
            URLEncoder.encode(from, StandardCharsets.UTF_8),
            URLEncoder.encode(to, StandardCharsets.UTF_8));
        
        url += String.format("?adjusted=%s&sort=%s&limit=%d&apikey=%s", 
            adjusted, sort, limit, Config.getPolygonApiKey());
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Polygon API error: " + response.statusCode() + " - " + response.body());
        }
        
        return parseAggregatesResponse(response.body());
    }
    
    private List<OhlcData> parseAggregatesResponse(String jsonResponse) throws IOException {
        JsonNode root = objectMapper.readTree(jsonResponse);
        List<OhlcData> ohlcDataList = new ArrayList<>();
        
        if (!root.has("results") || !root.get("results").isArray()) {
            throw new IOException("Invalid response format from Polygon API");
        }
        
        JsonNode results = root.get("results");
        double previousClose = 0.0;
        
        for (int i = 0; i < results.size(); i++) {
            JsonNode result = results.get(i);
            long timestamp = result.get("t").asLong();
            // Polygon returns epoch millis for the bar start; convert accurately to LocalDate
            java.time.LocalDate date = java.time.Instant
                .ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
            
            double open = result.get("o").asDouble();
            double high = result.get("h").asDouble();
            double low = result.get("l").asDouble();
            double close = result.get("c").asDouble();
            
            // Calculate percent return (current close / previous close)
            double percentReturn = (i == 0 || previousClose == 0.0) ? 1.0 : close / previousClose;
            
            ohlcDataList.add(new OhlcData(date, open, high, low, close, percentReturn));
            previousClose = close;
        }
        
        return ohlcDataList;
    }
    
    @Tool(description = "Retrieve OHLC data for a symbol or ratio with optional date range and limit. " +
          "Supports individual symbols (e.g., 'AAPL') and ratios (e.g., 'AAPL/SPY'). " +
          "Parameters: symbol ('AAPL' or 'AAPL/SPY'); period ('1D','1W','1M','3M','1Y'); " +
          "startDate ('YYYY-MM-DD') and endDate ('YYYY-MM-DD') override period if provided; " +
          "limit (max bars to return; default larger for full history). " +
          "Returns a formatted table: Date, Open, High, Low, Close, % Return.")
    public String getStockData(String symbol, String period, String startDate, String endDate, Integer limit) {
        try {
            // Parse period to determine the timeframe
            String multiplier = "1";
            String timespan = "day";
            
            // Calculate date range defaults
            LocalDate computedEnd = LocalDate.now();
            LocalDate computedStart = computedEnd.minusMonths(12); // default to last 1Y
            
            if (period != null && (startDate == null && endDate == null)) {
                switch (period.toUpperCase()) {
                    case "1D":
                        computedStart = computedEnd.minusDays(1);
                        timespan = "minute";
                        multiplier = "5";
                        break;
                    case "1W":
                        computedStart = computedEnd.minusWeeks(1);
                        break;
                    case "1M":
                        computedStart = computedEnd.minusMonths(1);
                        break;
                    case "3M":
                        computedStart = computedEnd.minusMonths(3);
                        break;
                    case "1Y":
                        computedStart = computedEnd.minusYears(1);
                        break;
                }
            }

            // Override with explicit dates if provided
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            if (startDate != null) {
                computedStart = LocalDate.parse(startDate, fmt);
            }
            if (endDate != null) {
                computedEnd = LocalDate.parse(endDate, fmt);
            }
            if (startDate != null && endDate == null) {
                // default end to today if only start provided
                computedEnd = LocalDate.now();
            }
            if (endDate != null && startDate == null) {
                // default start to 1Y before end if only end provided
                computedStart = computedEnd.minusYears(1);
            }

            // Determine limit
            int effectiveLimit = (limit != null && limit > 0) ? limit : ("minute".equals(timespan) ? 5000 : 50000);
            
            // Check if this is a ratio (contains "/")
            if (symbol.contains("/")) {
                return getRatioDataAsText(symbol, multiplier, timespan, 
                    computedStart.format(fmt),
                    computedEnd.format(fmt),
                    true, "asc", effectiveLimit);
            } else {
                return getStockDataAsText(symbol, multiplier, timespan, 
                    computedStart.format(fmt),
                    computedEnd.format(fmt),
                    true, "asc", effectiveLimit);
            }
        } catch (Exception e) {
            return "Error retrieving stock data: " + e.getMessage();
        }
    }

    public String getStockDataAsText(String ticker, String multiplier, String timespan, 
                                   String from, String to, boolean adjusted, String sort, int limit) {
        try {
            List<OhlcData> data = getAggregates(ticker, multiplier, timespan, from, to, adjusted, sort, limit);
            
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Stock data for %s (%s %s bars from %s to %s):\n\n", 
                ticker.toUpperCase(), multiplier, timespan, from, to));
            sb.append("Date       | Open     | High     | Low      | Close    | % Return\n");
            sb.append("-----------|----------|----------|----------|----------|----------\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (OhlcData ohlc : data) {
                double percentChange = (ohlc.getPercentReturn() - 1.0) * 100.0; // Convert to percentage
                sb.append(String.format("%-10s | %8.2f | %8.2f | %8.2f | %8.2f | %8.2f%%\n",
                    ohlc.getDate().format(formatter),
                    ohlc.getOpen(),
                    ohlc.getHigh(),
                    ohlc.getLow(),
                    ohlc.getClose(),
                    percentChange));
            }
            
            sb.append(String.format("\nTotal records: %d\n", data.size()));
            return sb.toString();
            
        } catch (Exception e) {
            return "Error fetching stock data: " + e.getMessage();
        }
    }
    
    public String getRatioDataAsText(String ratioSymbol, String multiplier, String timespan, 
                                   String from, String to, boolean adjusted, String sort, int limit) {
        try {
            String[] symbols = ratioSymbol.split("/");
            if (symbols.length != 2) {
                return "Invalid ratio format. Use SYMBOL1/SYMBOL2";
            }
            
            String numeratorSymbol = symbols[0].trim();
            String denominatorSymbol = symbols[1].trim();
            
            // Get data for both symbols
            List<OhlcData> numeratorData = getAggregates(numeratorSymbol, multiplier, timespan, from, to, adjusted, sort, limit);
            List<OhlcData> denominatorData = getAggregates(denominatorSymbol, multiplier, timespan, from, to, adjusted, sort, limit);
            
            // Create a map for denominator data for quick lookup
            java.util.Map<LocalDate, OhlcData> denominatorMap = new java.util.HashMap<>();
            for (OhlcData data : denominatorData) {
                denominatorMap.put(data.getDate(), data);
            }
            
            // Calculate ratio data
            List<OhlcData> ratioData = new ArrayList<>();
            double previousRatioClose = 0.0;
            
            for (OhlcData numData : numeratorData) {
                OhlcData denomData = denominatorMap.get(numData.getDate());
                if (denomData != null && denomData.getClose() != 0 && denomData.getOpen() != 0 && 
                    denomData.getHigh() != 0 && denomData.getLow() != 0) {
                    
                    double ratioOpen = numData.getOpen() / denomData.getOpen();
                    double ratioHigh = numData.getHigh() / denomData.getHigh();
                    double ratioLow = numData.getLow() / denomData.getLow();
                    double ratioClose = numData.getClose() / denomData.getClose();
                    
                    // Calculate percent return for ratio
                    double ratioPercentReturn = (ratioData.isEmpty() || previousRatioClose == 0.0) ? 
                        1.0 : ratioClose / previousRatioClose;
                    
                    ratioData.add(new OhlcData(numData.getDate(), ratioOpen, ratioHigh, ratioLow, ratioClose, ratioPercentReturn));
                    previousRatioClose = ratioClose;
                }
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Ratio data for %s (%s %s bars from %s to %s):\n\n", 
                ratioSymbol.toUpperCase(), multiplier, timespan, from, to));
            sb.append("Date       | Open     | High     | Low      | Close    | % Return\n");
            sb.append("-----------|----------|----------|----------|----------|----------\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (OhlcData ohlc : ratioData) {
                double percentChange = (ohlc.getPercentReturn() - 1.0) * 100.0; // Convert to percentage
                sb.append(String.format("%-10s | %8.4f | %8.4f | %8.4f | %8.4f | %8.2f%%\n",
                    ohlc.getDate().format(formatter),
                    ohlc.getOpen(),
                    ohlc.getHigh(),
                    ohlc.getLow(),
                    ohlc.getClose(),
                    percentChange));
            }
            
            sb.append(String.format("\nTotal records: %d\n", ratioData.size()));
            sb.append(String.format("Numerator: %s, Denominator: %s\n", numeratorSymbol, denominatorSymbol));
            return sb.toString();
            
        } catch (Exception e) {
            return "Error calculating ratio data: " + e.getMessage();
        }
    }
}

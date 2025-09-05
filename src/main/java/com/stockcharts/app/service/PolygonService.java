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
        for (JsonNode result : results) {
            long timestamp = result.get("t").asLong();
            LocalDate date = LocalDate.ofEpochDay(timestamp / (1000 * 60 * 60 * 24));
            
            double open = result.get("o").asDouble();
            double high = result.get("h").asDouble();
            double low = result.get("l").asDouble();
            double close = result.get("c").asDouble();
            
            ohlcDataList.add(new OhlcData(date, open, high, low, close));
        }
        
        return ohlcDataList;
    }
    
    @Tool(description = "Get stock market data for a given symbol including OHLC data and current price")
    public String getStockData(String symbol, String period) {
        try {
            // Parse period to determine the timeframe
            String multiplier = "1";
            String timespan = "day";
            
            // Calculate date range - default to last 30 days
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            
            if (period != null) {
                switch (period.toUpperCase()) {
                    case "1D":
                        startDate = endDate.minusDays(1);
                        timespan = "minute";
                        multiplier = "5";
                        break;
                    case "1W":
                        startDate = endDate.minusWeeks(1);
                        break;
                    case "1M":
                        startDate = endDate.minusMonths(1);
                        break;
                    case "3M":
                        startDate = endDate.minusMonths(3);
                        break;
                    case "1Y":
                        startDate = endDate.minusYears(1);
                        break;
                }
            }
            
            return getStockDataAsText(symbol, multiplier, timespan, 
                startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                true, "asc", 50);
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
            sb.append("Date       | Open     | High     | Low      | Close\n");
            sb.append("-----------|----------|----------|----------|----------\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (OhlcData ohlc : data) {
                sb.append(String.format("%-10s | %8.2f | %8.2f | %8.2f | %8.2f\n",
                    ohlc.getDate().format(formatter),
                    ohlc.getOpen(),
                    ohlc.getHigh(),
                    ohlc.getLow(),
                    ohlc.getClose()));
            }
            
            sb.append(String.format("\nTotal records: %d\n", data.size()));
            return sb.toString();
            
        } catch (Exception e) {
            return "Error fetching stock data: " + e.getMessage();
        }
    }
}

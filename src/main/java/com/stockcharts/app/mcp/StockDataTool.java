package com.stockcharts.app.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stockcharts.app.service.PolygonService;

public class StockDataTool {
    
    private final PolygonService polygonService;
    private final ObjectMapper objectMapper;
    
    public StockDataTool(PolygonService polygonService) {
        this.polygonService = polygonService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }
    
    public String getName() {
        return "get_stock_data";
    }
    
    public String getDescription() {
        return "Fetch OHLC stock price data from Polygon.io for a given ticker and date range. Returns formatted text data.";
    }
    
    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode ticker = objectMapper.createObjectNode();
        ticker.put("type", "string");
        ticker.put("description", "Stock ticker symbol");
        properties.set("ticker", ticker);
        
        ObjectNode from = objectMapper.createObjectNode();
        from.put("type", "string");
        from.put("description", "Start date YYYY-MM-DD");
        properties.set("from", from);
        
        ObjectNode to = objectMapper.createObjectNode();
        to.put("type", "string");
        to.put("description", "End date YYYY-MM-DD");
        properties.set("to", to);
        
        schema.set("properties", properties);
        
        ArrayNode required = objectMapper.createArrayNode();
        required.add("ticker");
        required.add("from"); 
        required.add("to");
        schema.set("required", required);
        
        return schema;
    }
    
    public JsonNode execute(JsonNode arguments) {
        try {
            String ticker = arguments.get("ticker").asText();
            String multiplier = arguments.has("multiplier") ? arguments.get("multiplier").asText() : "1";
            String timespan = arguments.has("timespan") ? arguments.get("timespan").asText() : "day";
            String from = arguments.get("from").asText();
            String to = arguments.get("to").asText();
            boolean adjusted = arguments.has("adjusted") ? arguments.get("adjusted").asBoolean() : true;
            String sort = arguments.has("sort") ? arguments.get("sort").asText() : "asc";
            int limit = arguments.has("limit") ? arguments.get("limit").asInt() : 120;
            
            String stockData = polygonService.getStockDataAsText(ticker, multiplier, timespan, from, to, adjusted, sort, limit);
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("type", "text");
            result.put("text", stockData);
            return result;
                
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch stock data: " + e.getMessage(), e);
        }
    }
}
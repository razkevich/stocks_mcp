package com.stockcharts.app.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stockcharts.app.model.ChartRequest;
import com.stockcharts.app.service.ChartService;

import java.util.Base64;

public class StockChartTool {
    
    private final ChartService chartService;
    private final ObjectMapper objectMapper;
    
    public StockChartTool(ChartService chartService) {
        this.chartService = chartService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }
    
    public String getName() {
        return "generate_stock_chart";
    }
    
    public String getDescription() {
        return "Generate a stock chart image from OHLC data and custom lines. Returns base64 encoded JPEG image.";
    }
    
    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode ohlcData = objectMapper.createObjectNode();
        ohlcData.put("type", "array");
        ohlcData.put("description", "Array of OHLC data points");
        properties.set("ohlcData", ohlcData);
        
        ObjectNode width = objectMapper.createObjectNode();
        width.put("type", "integer");
        width.put("default", 800);
        properties.set("width", width);
        
        ObjectNode height = objectMapper.createObjectNode();
        height.put("type", "integer"); 
        height.put("default", 600);
        properties.set("height", height);
        
        schema.set("properties", properties);
        
        ArrayNode required = objectMapper.createArrayNode();
        required.add("ohlcData");
        schema.set("required", required);
        
        return schema;
    }
    
    public JsonNode execute(JsonNode arguments) {
        try {
            ChartRequest request = objectMapper.convertValue(arguments, ChartRequest.class);
            byte[] chartBytes = chartService.generateChart(request);
            String base64Image = Base64.getEncoder().encodeToString(chartBytes);
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("type", "text");
            result.put("text", "Chart generated successfully. Base64 JPEG data: " + base64Image);
            return result;
                
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate chart: " + e.getMessage(), e);
        }
    }
}
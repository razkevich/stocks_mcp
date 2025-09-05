package com.stockcharts.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stockcharts.app.mcp.McpMessage;
import com.stockcharts.app.mcp.StockChartTool;
import com.stockcharts.app.mcp.StockDataTool;
import com.stockcharts.app.mcp.TechnicalIndicatorTool;
import com.stockcharts.app.mcp.RatioTool;
import com.stockcharts.app.service.ChartService;
import com.stockcharts.app.service.PolygonService;
import com.stockcharts.app.service.IndicatorService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class StockChartsMcpServer {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void main(String[] args) {
        try {
            objectMapper.findAndRegisterModules();
            
            ChartService chartService = new ChartService();
            PolygonService polygonService = new PolygonService();
            IndicatorService indicatorService = new IndicatorService();
            StockChartTool chartTool = new StockChartTool(chartService);
            StockDataTool dataTool = new StockDataTool(polygonService);
            TechnicalIndicatorTool indicatorTool = new TechnicalIndicatorTool(indicatorService);
            RatioTool ratioTool = new RatioTool();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            
            while ((line = reader.readLine()) != null) {
                try {
                    McpMessage request = objectMapper.readValue(line, McpMessage.class);
                    McpMessage response = handleRequest(request, chartTool, dataTool, indicatorTool, ratioTool);
                    
                    if (response != null) {
                        System.out.println(objectMapper.writeValueAsString(response));
                        System.out.flush();
                    }
                } catch (Exception e) {
                    McpMessage errorResponse = McpMessage.error("unknown", -32603, "Internal error: " + e.getMessage());
                    System.out.println(objectMapper.writeValueAsString(errorResponse));
                    System.out.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start MCP server: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static McpMessage handleRequest(McpMessage request, StockChartTool chartTool, StockDataTool dataTool, TechnicalIndicatorTool indicatorTool, RatioTool ratioTool) throws Exception {
        String method = request.getMethod();
        
        if ("initialize".equals(method)) {
            ObjectNode capabilities = objectMapper.createObjectNode().put("tools", true);
            ObjectNode serverInfo = objectMapper.createObjectNode()
                .put("name", "stockcharts-mcp")
                .put("version", "1.0.0");
            ObjectNode result = objectMapper.createObjectNode()
                .put("protocolVersion", "2024-11-05");
            result.set("capabilities", capabilities);
            result.set("serverInfo", serverInfo);
            return McpMessage.response(request.getId(), result);
        }
        
        if ("tools/list".equals(method)) {
            ArrayNode tools = objectMapper.createArrayNode();
            ObjectNode chartToolNode = objectMapper.createObjectNode()
                .put("name", chartTool.getName())
                .put("description", chartTool.getDescription());
            chartToolNode.set("inputSchema", chartTool.getInputSchema());
            tools.add(chartToolNode);
            
            ObjectNode dataToolNode = objectMapper.createObjectNode()
                .put("name", dataTool.getName())
                .put("description", dataTool.getDescription());
            dataToolNode.set("inputSchema", dataTool.getInputSchema());
            tools.add(dataToolNode);
            
            ObjectNode indicatorToolNode = objectMapper.createObjectNode()
                .put("name", indicatorTool.getName())
                .put("description", indicatorTool.getDescription());
            indicatorToolNode.set("inputSchema", indicatorTool.getInputSchema());
            tools.add(indicatorToolNode);

            ObjectNode ratioToolNode = objectMapper.createObjectNode()
                .put("name", ratioTool.getName())
                .put("description", ratioTool.getDescription());
            ratioToolNode.set("inputSchema", ratioTool.getInputSchema());
            tools.add(ratioToolNode);
                    
            ObjectNode result = objectMapper.createObjectNode();
            result.set("tools", tools);
            return McpMessage.response(request.getId(), result);
        }
        
        if ("tools/call".equals(method)) {
            JsonNode params = request.getParams();
            String toolName = params.get("name").asText();
            JsonNode arguments = params.get("arguments");
            
            JsonNode toolResult;
            if ("generate_stock_chart".equals(toolName)) {
                toolResult = chartTool.execute(arguments);
            } else if ("get_stock_data".equals(toolName)) {
                toolResult = dataTool.execute(arguments);
            } else if ("calculate_technical_indicator".equals(toolName)) {
                toolResult = indicatorTool.execute(arguments);
            } else if ("calculate_ratio".equals(toolName)) {
                toolResult = ratioTool.execute(arguments);
            } else {
                return McpMessage.error(request.getId(), -32601, "Tool not found: " + toolName);
            }
            
            ArrayNode content = objectMapper.createArrayNode().add(toolResult);
            ObjectNode result = objectMapper.createObjectNode();
            result.set("content", content);
            return McpMessage.response(request.getId(), result);
        }
        
        return McpMessage.error(request.getId(), -32601, "Method not found: " + method);
    }
}

package com.stockcharts.app.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stockcharts.app.model.OhlcData;
import com.stockcharts.app.service.IndicatorService;
import com.stockcharts.app.service.IndicatorService.IndicatorValue;
import com.stockcharts.app.service.IndicatorService.MacdValue;

import java.text.DecimalFormat;
import java.util.List;

public class TechnicalIndicatorTool {

    private final IndicatorService indicatorService;
    private final ObjectMapper objectMapper;

    public TechnicalIndicatorTool(IndicatorService indicatorService) {
        this.indicatorService = indicatorService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public String getName() { return "calculate_technical_indicator"; }

    public String getDescription() {
        return "Calculate a technical indicator (sma, macd, rsi) from OHLC data. Returns a formatted text table.";
    }

    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode data = objectMapper.createObjectNode();
        data.put("type", "array");
        data.put("description", "Array of OHLC data points");
        properties.set("ohlcData", data);

        ObjectNode op = objectMapper.createObjectNode();
        op.put("type", "string");
        op.put("description", "Operation: sma, macd, rsi");
        ArrayNode enums = objectMapper.createArrayNode();
        enums.add("sma");
        enums.add("macd");
        enums.add("rsi");
        op.set("enum", enums);
        properties.set("operation", op);

        ObjectNode period = objectMapper.createObjectNode();
        period.put("type", "integer");
        period.put("description", "Period for SMA/RSI (default: SMA 20, RSI 14)");
        properties.set("period", period);

        ObjectNode fast = objectMapper.createObjectNode();
        fast.put("type", "integer");
        fast.put("description", "MACD fast period (default 12)");
        properties.set("fastPeriod", fast);

        ObjectNode slow = objectMapper.createObjectNode();
        slow.put("type", "integer");
        slow.put("description", "MACD slow period (default 26)");
        properties.set("slowPeriod", slow);

        ObjectNode signal = objectMapper.createObjectNode();
        signal.put("type", "integer");
        signal.put("description", "MACD signal period (default 9)");
        properties.set("signalPeriod", signal);

        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("ohlcData");
        required.add("operation");
        schema.set("required", required);

        return schema;
    }

    public JsonNode execute(JsonNode arguments) {
        try {
            List<OhlcData> data = objectMapper.convertValue(arguments.get("ohlcData"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OhlcData.class));
            String operation = arguments.get("operation").asText().toLowerCase();

            DecimalFormat df = new DecimalFormat("0.0000");
            StringBuilder sb = new StringBuilder();

            switch (operation) {
                case "sma": {
                    int period = arguments.has("period") ? arguments.get("period").asInt() : 20;
                    List<IndicatorValue> sma = indicatorService.sma(data, period);
                    sb.append("Date       | SMA(").append(period).append(")\n");
                    sb.append("-----------|-----------\n");
                    for (IndicatorValue v : sma) {
                        sb.append(String.format("%-10s | %s\n", v.getDate().toString(), df.format(v.getValue())));
                    }
                    break;
                }
                case "rsi": {
                    int period = arguments.has("period") ? arguments.get("period").asInt() : 14;
                    List<IndicatorValue> rsi = indicatorService.rsi(data, period);
                    sb.append("Date       | RSI(").append(period).append(")\n");
                    sb.append("-----------|-----------\n");
                    for (IndicatorValue v : rsi) {
                        sb.append(String.format("%-10s | %s\n", v.getDate().toString(), df.format(v.getValue())));
                    }
                    break;
                }
                case "macd": {
                    int fast = arguments.has("fastPeriod") ? arguments.get("fastPeriod").asInt() : 12;
                    int slow = arguments.has("slowPeriod") ? arguments.get("slowPeriod").asInt() : 26;
                    int signal = arguments.has("signalPeriod") ? arguments.get("signalPeriod").asInt() : 9;
                    List<MacdValue> macd = indicatorService.macd(data, fast, slow, signal);
                    sb.append("Date       | MACD     | Signal   | Hist\n");
                    sb.append("-----------|----------|----------|----------\n");
                    for (MacdValue v : macd) {
                        sb.append(String.format("%-10s | %s | %s | %s\n",
                                v.getDate().toString(), df.format(v.getMacd()), df.format(v.getSignal()), df.format(v.getHistogram())));
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported operation: " + operation);
            }

            ObjectNode result = objectMapper.createObjectNode();
            result.put("type", "text");
            result.put("text", sb.toString());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate indicator: " + e.getMessage(), e);
        }
    }
}


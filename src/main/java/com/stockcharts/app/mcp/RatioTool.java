package com.stockcharts.app.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stockcharts.app.model.OhlcData;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatioTool {

    private final ObjectMapper objectMapper;

    public RatioTool() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public String getName() { return "calculate_ratio"; }

    public String getDescription() {
        return "Compute OHLC ratios of two datasets (ohlcData1 / ohlcData2) aligned by date.";
    }

    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode d1 = objectMapper.createObjectNode();
        d1.put("type", "array");
        d1.put("description", "First OHLC dataset");
        properties.set("ohlcData1", d1);

        ObjectNode d2 = objectMapper.createObjectNode();
        d2.put("type", "array");
        d2.put("description", "Second OHLC dataset");
        properties.set("ohlcData2", d2);

        ObjectNode op = objectMapper.createObjectNode();
        op.put("type", "string");
        op.put("description", "Operation: ratio");
        ArrayNode enums = objectMapper.createArrayNode();
        enums.add("ratio");
        op.set("enum", enums);
        properties.set("operation", op);

        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("ohlcData1");
        required.add("ohlcData2");
        required.add("operation");
        schema.set("required", required);

        return schema;
    }

    public JsonNode execute(JsonNode arguments) {
        try {
            if (!arguments.get("operation").asText().equalsIgnoreCase("ratio")) {
                throw new IllegalArgumentException("Unsupported operation: " + arguments.get("operation").asText());
            }

            List<OhlcData> d1 = objectMapper.convertValue(arguments.get("ohlcData1"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OhlcData.class));
            List<OhlcData> d2 = objectMapper.convertValue(arguments.get("ohlcData2"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OhlcData.class));

            Map<LocalDate, OhlcData> map2 = new HashMap<>();
            for (OhlcData o : d2) map2.put(o.getDate(), o);

            DecimalFormat df = new DecimalFormat("0.0000");
            StringBuilder sb = new StringBuilder();
            sb.append("Date       | OpenR    | HighR    | LowR     | CloseR\n");
            sb.append("-----------|----------|----------|----------|----------\n");

            for (OhlcData a : d1) {
                OhlcData b = map2.get(a.getDate());
                if (b == null) continue; // skip non-overlapping dates
                double openR = ratio(a.getOpen(), b.getOpen());
                double highR = ratio(a.getHigh(), b.getHigh());
                double lowR = ratio(a.getLow(), b.getLow());
                double closeR = ratio(a.getClose(), b.getClose());
                sb.append(String.format("%-10s | %s | %s | %s | %s\n",
                        a.getDate().toString(), df.format(openR), df.format(highR), df.format(lowR), df.format(closeR)));
            }

            ObjectNode result = objectMapper.createObjectNode();
            result.put("type", "text");
            result.put("text", sb.toString());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate ratio: " + e.getMessage(), e);
        }
    }

    private static double ratio(double a, double b) {
        if (b == 0.0) return Double.NaN;
        return a / b;
    }
}


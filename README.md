# Stock Charts MCP Server

A Java application that provides both REST API and MCP (Model Context Protocol) server functionality for generating stock charts and fetching stock data from Polygon.io.

## Features

- **Chart Generation**: Create minimal candlestick charts with custom overlay lines using JFreeChart
- **Stock Data**: Fetch OHLC stock price data from Polygon.io  
- **REST API**: Traditional HTTP endpoints for web integration
- **MCP Server**: Protocol-compliant server for AI agent integration

## Technologies Used

- **Java 17**
- **Spring Boot** - REST API framework
- **JFreeChart** - Chart generation library
- **Polygon.io API** - Stock data source
- **Jackson** - JSON processing
- **MCP Protocol** - AI agent communication

## API Endpoints

### REST API (Spring Boot)

#### Generate Stock Chart
```http
POST /api/chart/generate
Content-Type: application/json

{
  "ohlcData": [
    {
      "date": "2024-01-01",
      "open": 100.0,
      "high": 110.0, 
      "low": 95.0,
      "close": 105.0
    }
  ],
  "lines": [
    {
      "startDate": "2024-01-01",
      "endDate": "2024-01-05",
      "startValue": 100.0,
      "endValue": 125.0,
      "color": "#FF0000",
      "strokeWidth": 2.0
    }
  ],
  "width": 800,
  "height": 600
}
```

#### Get Stock Data  
```http
GET /api/stock/data?ticker=AAPL&from=2024-01-01&to=2024-01-05
```

### MCP Server

The MCP server provides two tools:

1. **generate_stock_chart** - Generate chart images from OHLC data
2. **get_stock_data** - Fetch stock price data from Polygon.io

## Running the Application

### REST API Server (Default)
```bash
mvn spring-boot:run
```
Server starts on http://localhost:8080

### MCP Server Mode
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--mcp
```

Or alternatively:
```bash
mvn compile exec:java -Dexec.mainClass="com.stockcharts.app.StockChartsApplication" -Dexec.args="--mcp"
```

## Testing

### Test REST Endpoints
```bash
# Test stock data endpoint
curl "http://localhost:8080/api/stock/data?ticker=AAPL&from=2024-01-01&to=2024-01-05"

# Test chart generation
curl -X POST http://localhost:8080/api/chart/generate \
  -H "Content-Type: application/json" \
  -d @test-request.json \
  --output chart.jpg
```

### Test MCP Server
```bash
./test-mcp.sh
```

## Configuration

### Polygon.io API Key
The API key is hardcoded in `PolygonService.java`:
```java
private static final String API_KEY = "RGdVqZNHE_Iryopre7gfYYd7YESCakmY";
```

### Chart Styling
Charts are generated with minimal styling (no axes, grids, or titles) showing only candlesticks and custom lines.

## Dependencies

```xml
<dependency>
    <groupId>org.jfree</groupId>
    <artifactId>jfreechart</artifactId>
    <version>1.5.3</version>
</dependency>
```

## MCP Integration

To use with Claude Code or other MCP clients, configure the server in your MCP settings:

```json
{
  "mcpServers": {
    "stockcharts": {
      "command": "java",
      "args": ["-cp", "target/classes", "com.stockcharts.app.StockChartsMcpServer"]
    }
  }
}
```
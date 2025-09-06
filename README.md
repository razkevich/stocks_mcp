# Stock Charts MCP Server

A Java application that provides an MCP (Model Context Protocol) server for generating stock charts, computing indicators, and fetching stock data from Polygon.io.

## Features

- **Chart Generation**: Create minimal candlestick charts with custom overlay lines using JFreeChart
- **Stock Data**: Fetch OHLC stock price data from Polygon.io
- **Technical Indicators**: SMA, RSI, MACD over OHLC data
- **Ratios**: Compute OHLC ratios between two datasets
- **MCP Server**: Protocol-compliant server for AI agent integration

## Technologies Used

- **Java 17**
- **JFreeChart** - Chart generation library
- **Polygon.io API** - Stock data source
- **Jackson** - JSON processing
- **MCP Protocol** - AI agent communication

## MCP Server

The MCP server provides tools:

1. **generate_stock_chart** - Generate chart images from OHLC data
2. **get_stock_data** - Fetch stock price data from Polygon.io
   - params: `symbol` (supports ratios `AAPL/SPY`), `period` (`1D`,`1W`,`1M`,`3M`,`1Y`)
   - optional: `startDate` (`YYYY-MM-DD`), `endDate` (`YYYY-MM-DD`) override `period`
   - optional: `limit` (max bars; default larger to allow full history)
   - returns: formatted table of Date, Open, High, Low, Close
3. **calculate_technical_indicator** - Compute indicators over OHLC data
   - operation: `sma` (period, default 20), `rsi` (period, default 14), `macd` (fastPeriod 12, slowPeriod 26, signalPeriod 9)
   - input: `ohlcData` array
   - returns: formatted text table
4. **calculate_ratio** - Compute OHLC ratios of two datasets
   - operation: `ratio`
   - input: `ohlcData1`, `ohlcData2` arrays
   - returns: formatted text table of open/high/low/close ratios aligned by date

## Running the Application

### MCP Server
```bash
mvn compile exec:java -Dexec.mainClass="com.stockcharts.app.StockChartsMcpServer"
```

## Testing

### Test MCP Server
```bash
./test-mcp.sh
```

## Configuration

### Polygon.io API Key
Create a local config file and set your key (do not commit this file):
```
config/local.properties
```
Contents:
```
POLYGON_API_KEY=your_polygon_api_key_here
```

Alternatively, set an environment variable:
```
export POLYGON_API_KEY=your_polygon_api_key_here
```

The application reads the key from the env var first, then from `config/local.properties`.

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

To use with Codex CLI or other MCP clients, configure the server in your MCP settings:

```json
{
  "mcpServers": {
    "stockcharts": {
      "command": "java",
      "args": ["-jar", "target/stockcharts-app-1.0.0-shaded.jar"],
      "env": {
        "POLYGON_API_KEY": "your_polygon_api_key_here"
      },
      "cwd": "/absolute/path/to/this/repo"
    }
  }
}
```

Build the shaded JAR once before connecting:
```
mvn package -DskipTests
```
Running via `java -jar` ensures no extra stdout noise (which can break MCP framing) compared to running through `mvn exec`.

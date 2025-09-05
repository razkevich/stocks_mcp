#!/bin/bash

# Test script for MCP server
echo "Testing Stock Charts MCP Server"
echo "==============================="

# Start the MCP server and send test messages
java -cp target/classes com.stockcharts.app.StockChartsMcpServer &
SERVER_PID=$!

sleep 2

# Test initialize
echo '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}' | java -cp target/classes com.stockcharts.app.StockChartsMcpServer

# Clean up
kill $SERVER_PID 2>/dev/null || true

echo "Test completed"
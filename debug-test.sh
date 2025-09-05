#!/bin/bash

echo "Debug test for MCP server"
echo "========================="

# Test 1: Check if JAR exists
if [ -f "target/stockcharts-app-1.0.0-shaded.jar" ]; then
    echo "✓ Shaded JAR exists"
else
    echo "✗ Shaded JAR missing"
    exit 1
fi

# Test 2: Try to run the server with a proper MCP initialize message
echo "Sending initialize message..."

(
    echo -e "Content-Length: 60\r"
    echo -e "Content-Type: application/json\r"
    echo -e "\r"
    echo -n '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}'
) | timeout 3 java -jar target/stockcharts-app-1.0.0-shaded.jar

echo
echo "Test completed"
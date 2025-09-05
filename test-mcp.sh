#!/bin/bash

# Test script for MCP server
echo "Testing Stock Charts MCP Server"
echo "==============================="

# Build shaded JAR (offline cache assumed). Then start server and send framed initialize.
mvn -q -o -DskipTests package >/dev/null 2>&1 || true
(
    echo -e "Content-Length: 60\r"
    echo -e "Content-Type: application/json\r"
    echo -e "\r"
    echo -n '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}'
) | java -jar target/stockcharts-app-1.0.0-shaded.jar

echo "Test completed"

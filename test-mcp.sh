#!/bin/bash

# MCP Protocol Test Script
BASE_URL="http://localhost:8080/mcp"

echo "==================================="
echo "MCP Protocol Test"
echo "==================================="

# Test 1: Initialize
echo -e "\n[1] Testing initialize..."
curl -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    }
  }' | jq '.'

# Test 2: Initialized notification
echo -e "\n[2] Testing initialized notification..."
curl -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "jsonrpc": "2.0",
    "method": "notifications/initialized"
  }' | jq '.'

# Test 3: Tools/list
echo -e "\n[3] Testing tools/list..."
curl -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "jsonrpc": "2.0",
    "id": "2",
    "method": "tools/list"
  }' | jq '.'

# Test 4: Ping
echo -e "\n[4] Testing ping..."
curl -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "jsonrpc": "2.0",
    "id": "3",
    "method": "ping"
  }' | jq '.'

# Test 5: Invalid method
echo -e "\n[5] Testing invalid method..."
curl -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "jsonrpc": "2.0",
    "id": "4",
    "method": "invalid_method"
  }' | jq '.'

echo -e "\n==================================="
echo "Test completed"
echo "==================================="

#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    echo "Loading environment variables from .env..."
    export $(grep -v '^#' .env | xargs)
else
    echo "Warning: .env file not found"
fi

# Kill existing server process
echo "Stopping existing server..."
pkill -f 'atlassian-mcp-java' || echo "No existing server process found"

# Wait for process to terminate
sleep 1

# Start server in background with log output
echo "Starting server..."
nohup java -jar target/atlassian-mcp-java-0.1.0-SNAPSHOT.jar > logs/server.log 2>&1 &

# Wait a bit for server to start
sleep 3

# Check if server is running
if pgrep -f 'atlassian-mcp-java' > /dev/null; then
    echo "✅ Server started successfully"
    echo "📋 Log file: logs/server.log"
    echo "🔗 Server URL: http://localhost:8080"
    tail -5 logs/server.log
else
    echo "❌ Server failed to start. Check logs/server.log"
    tail -20 logs/server.log
    exit 1
fi

#!/bin/bash
while true; do
    clear
    echo "======================================"
    echo "  JMeter Redis Cache Test - LIVE"
    echo "======================================"
    echo ""
    tail -5 jmeter_output.log | grep summary
    echo ""
    echo "Redis Stats:"
    redis-cli DBSIZE | xargs echo "  Cached keys:"
    redis-cli INFO stats | grep "total_commands_processed\|keyspace_hits\|keyspace_misses" | sed 's/^/  /'
    echo ""
    echo "App Status:"
    ps aux | grep "mopl-api-0.0.1-SNAPSHOT.jar" | grep -v grep | awk '{printf "  PID %s, CPU %.1f%%, Mem %.1f%%\n", $2, $3, $4}'
    echo ""
    echo "Press Ctrl+C to stop monitoring"
    sleep 10
done

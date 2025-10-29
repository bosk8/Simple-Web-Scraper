# Simple Web Scraper - Runbook

This runbook provides operational guidance for using the Simple Web Scraper in production environments.

## Pre-Flight Checklist

Before running the scraper, ensure:

- [ ] Java 17+ is installed and accessible
- [ ] Target websites are accessible from your network
- [ ] You have permission to scrape the target websites
- [ ] Output directory has sufficient disk space
- [ ] Network bandwidth is adequate for your scraping volume

## Configuration

### Environment Variables

Set these environment variables for production use:

```bash
export JAVA_OPTS="-Xmx2g -Xms512m"  # Adjust memory as needed
export SCRAPER_LOG_LEVEL="INFO"      # DEBUG, INFO, WARN, ERROR
export SCRAPER_USER_AGENT="MyCompanyBot/1.0 (mailto:contact@mycompany.com)"
```

### Configuration File

Create `scraper-config.json` for complex scraping jobs:

```json
{
  "startUrls": [
    "https://example.com/products",
    "https://example.com/categories"
  ],
  "selectors": {
    "container": "article, .item, .product",
    "title": "h1, h2, h3, .title",
    "description": "p, .description",
    "url": "a",
    "price": ".price, .cost",
    "image": "img"
  },
  "rateLimitMs": 2000,
  "userAgent": "MyCompanyBot/1.0 (mailto:contact@mycompany.com)",
  "respectRobots": true
}
```

## Usage Patterns

### Single Site Scraping

```bash
# Basic scraping with default settings
java -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar \
  --url https://example.com \
  --output example-data.csv

# High-volume scraping with custom rate limiting
java -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar \
  --url https://example.com \
  --delay-ms 500 \
  --max-pages 1000 \
  --output large-dataset.csv
```

### Multi-Site Scraping

```bash
# Scrape multiple sites with different configurations
java -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar \
  --url https://site1.com \
  --url https://site2.com \
  --url https://site3.com \
  --delay-ms 2000 \
  --format jsonl \
  --output multi-site-data.jsonl
```

### Batch Processing

```bash
# Process URLs from a file
cat urls.txt | while read url; do
  java -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar \
    --url "$url" \
    --output "data/$(basename "$url").csv" \
    --delay-ms 3000
done
```

## Monitoring and Logging

### Log Levels

- **DEBUG**: Detailed request/response information
- **INFO**: General operation information (default)
- **WARN**: Non-fatal issues (rate limits, robots.txt warnings)
- **ERROR**: Fatal errors requiring attention

### Key Metrics to Monitor

1. **Success Rate**: Percentage of successful page fetches
2. **Rate Limit Hits**: Frequency of 429 responses
3. **Robots.txt Violations**: Sites blocking your scraper
4. **Error Patterns**: Common failure modes
5. **Data Quality**: Percentage of pages with extracted data

### Log Analysis

```bash
# Count successful requests
grep "Successfully fetched" scraper.log | wc -l

# Find rate limit issues
grep "Rate limited" scraper.log

# Check robots.txt violations
grep "Crawling disallowed" scraper.log

# Monitor error patterns
grep "ERROR" scraper.log | tail -20
```

## Performance Tuning

### Memory Optimization

```bash
# For large-scale scraping
java -Xmx4g -Xms1g -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar \
  --url https://example.com \
  --max-pages 10000
```

### Rate Limiting Guidelines

| Site Type | Recommended Delay | Notes |
|-----------|-------------------|-------|
| News Sites | 1-2 seconds | Usually allow scraping |
| E-commerce | 2-3 seconds | May have anti-bot measures |
| Social Media | 5+ seconds | Often restrict scraping |
| Government | 1-2 seconds | Usually public data |
| Corporate | 3-5 seconds | Check robots.txt carefully |

### Concurrent Requests

The scraper automatically limits concurrent requests per domain (max 2). For multiple domains, you can run multiple instances:

```bash
# Run multiple scrapers in parallel for different domains
java -jar scraper.jar --url https://site1.com --output site1.csv &
java -jar scraper.jar --url https://site2.com --output site2.csv &
java -jar scraper.jar --url https://site3.com --output site3.csv &
wait
```

## Error Handling

### Common Error Scenarios

1. **Connection Timeouts**
   ```bash
   # Increase timeout and delay
   java -jar scraper.jar --url https://slow-site.com --delay-ms 5000
   ```

2. **Rate Limiting (429)**
   ```bash
   # Increase delay between requests
   java -jar scraper.jar --url https://rate-limited-site.com --delay-ms 10000
   ```

3. **Robots.txt Violations**
   ```bash
   # Check robots.txt manually, consider using --no-respect-robots
   curl https://example.com/robots.txt
   ```

4. **Server Errors (5xx)**
   - The scraper automatically retries with exponential backoff
   - Monitor logs for persistent failures

### Recovery Procedures

1. **Partial Data Recovery**
   ```bash
   # Resume from last successful page
   tail -1 output.csv  # Find last processed URL
   # Restart scraper with remaining URLs
   ```

2. **Data Validation**
   ```bash
   # Check data quality
   wc -l output.csv  # Count records
   grep -c "title" output.csv  # Count records with titles
   ```

## Data Management

### Output File Rotation

The scraper automatically rotates files when they exceed 10MB:

```bash
# Monitor file sizes
ls -lh output*.csv
# Files will be named: output.csv, output-1.csv, output-2.csv, etc.
```

### Data Validation

```bash
# Validate CSV format
head -5 output.csv
# Check for proper encoding
file output.csv

# Validate JSONL format
head -5 output.jsonl | jq .
```

### Data Processing

```bash
# Convert CSV to JSONL
csvjson output.csv > output.jsonl

# Filter data by specific criteria
grep "specific-pattern" output.csv > filtered.csv

# Count unique values
cut -d',' -f1 output.csv | sort | uniq -c
```

## Security Considerations

### Network Security

- Use VPN or proxy if scraping from restricted networks
- Monitor for IP blocking or rate limiting
- Consider using rotating IP addresses for large-scale scraping

### Data Security

- Encrypt sensitive output files
- Use secure file transfer for data collection
- Implement proper access controls for scraped data

### Legal Compliance

- Always check robots.txt before scraping
- Respect website terms of service
- Implement proper data retention policies
- Consider GDPR/privacy implications for personal data

## Troubleshooting

### Debug Mode

```bash
# Enable debug logging
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
  -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar \
  --url https://example.com
```

### Common Issues

1. **"No data extracted"**
   - Check CSS selectors against target website
   - Verify website structure hasn't changed
   - Test with a single page first

2. **"Connection refused"**
   - Check network connectivity
   - Verify target URL is correct
   - Check for firewall restrictions

3. **"Out of memory"**
   - Increase heap size: `-Xmx4g`
   - Reduce concurrent requests
   - Process data in smaller batches

### Performance Issues

1. **Slow scraping**
   - Reduce delay between requests (carefully)
   - Check network latency
   - Monitor target server performance

2. **High memory usage**
   - Increase heap size
   - Check for memory leaks in logs
   - Restart scraper periodically

## Maintenance

### Regular Tasks

- [ ] Monitor scraper performance weekly
- [ ] Update CSS selectors as websites change
- [ ] Review and update rate limiting settings
- [ ] Clean up old output files
- [ ] Check for scraper updates

### Health Checks

```bash
# Test scraper functionality
java -jar scraper.jar --url https://httpbin.org/html --output test.csv

# Verify output quality
head -5 test.csv
rm test.csv
```

## Support

For operational issues:

1. Check this runbook first
2. Review log files for error patterns
3. Test with a simple URL to isolate issues
4. Contact support with:
   - Error logs
   - Target URL
   - Configuration used
   - Expected vs actual behavior

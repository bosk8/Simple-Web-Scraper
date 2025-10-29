# Simple Web Scraper

A robust and ethical Java web scraper that respects robots.txt rules, implements proper rate limiting, and provides both CSV and JSONL output formats.

## Features

- **Robots.txt Compliance**: Automatically fetches and respects robots.txt rules
- **Rate Limiting**: Configurable delays between requests to prevent server overload
- **Multiple Output Formats**: CSV and JSONL (JSON Lines) support
- **Robust Error Handling**: Exponential backoff for server errors, graceful degradation
- **HTTP/2 Support**: Modern HTTP client with connection pooling
- **Comprehensive Testing**: Unit, integration, and end-to-end tests
- **Command Line Interface**: Easy-to-use CLI with extensive configuration options

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+ (for building from source)

### Installation

1. **Download the executable JAR**:
   ```bash
   # The fat JAR includes all dependencies
   java -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar --help
   ```

2. **Build from source**:
   ```bash
   git clone <repository-url>
   cd simple-web-scraper
   mvn clean package
   ```

### Basic Usage

```bash
# Scrape a single URL
java -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar --url https://example.com

# Scrape multiple URLs with custom output
java -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar \
  --url https://site1.com \
  --url https://site2.com \
  --output data.csv \
  --format csv

# Use JSONL format with custom delay
java -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar \
  --url https://example.com \
  --format jsonl \
  --delay-ms 2000 \
  --output results.jsonl
```

## Command Line Options

### Required Options
- `--url <url>`: URL to scrape (can be specified multiple times)

### Optional Options
- `--max-pages <number>`: Maximum number of pages to scrape (default: unlimited)
- `--output <file>`: Output file path (default: output.csv)
- `--format <format>`: Output format: csv or jsonl (default: csv)
- `--delay-ms <milliseconds>`: Minimum delay between requests (default: 1000)
- `--user-agent <string>`: Custom User-Agent string
- `--respect-robots`: Respect robots.txt rules (default: true)
- `--no-respect-robots`: Ignore robots.txt rules
- `--help, -h`: Show help message

## Output Formats

### CSV Format
```csv
title,description,url,price,image_url
"Product Name","Product description","https://example.com/product","$99.99","https://example.com/image.jpg"
```

### JSONL Format
```jsonl
{"title":"Product Name","description":"Product description","url":"https://example.com/product","price":"$99.99","image_url":"https://example.com/image.jpg"}
{"title":"Another Product","description":"Another description","url":"https://example.com/product2","price":"$149.99","image_url":"https://example.com/image2.jpg"}
```

## Configuration

The scraper uses default CSS selectors that work with common website structures:

- **Container**: `article, .item, .product, .post`
- **Title**: `h1, h2, h3, .title, .name`
- **Description**: `p, .description, .summary`
- **URL**: `a`
- **Price**: `.price, .cost`
- **Image**: `img`

## Ethical Guidelines

This scraper is designed with ethical web scraping principles:

1. **Respect robots.txt**: Always check and follow robots.txt rules
2. **Rate Limiting**: Use appropriate delays between requests (default: 1 second)
3. **User-Agent Identification**: Identifies itself clearly with contact information
4. **Error Handling**: Gracefully handles server errors without overwhelming the target
5. **Resource Conservation**: Efficient connection pooling and resource management

### Recommended Practices

- Start with longer delays (2-3 seconds) for unknown sites
- Monitor server response codes and adjust accordingly
- Respect 429 (Too Many Requests) responses
- Use appropriate User-Agent strings with contact information
- Test on a small scale before large-scale scraping

## Development

### Project Structure

```
src/
├── main/java/com/example/scraper/
│   ├── core/
│   │   ├── http/          # HTTP client implementation
│   │   ├── parser/        # HTML parsing with jsoup
│   │   ├── persistence/   # CSV/JSONL writers
│   │   └── robots/        # robots.txt compliance
│   ├── cli/               # Command-line interface
│   └── model/             # Data models
└── test/
    ├── java/              # Unit and integration tests
    └── resources/fixtures/ # Test HTML fixtures
```

### Building

```bash
# Compile, test, and run static analysis
mvn clean verify

# Create executable JAR
mvn clean package

# Run specific tests
mvn test -Dtest=HtmlParserTest
```

### Testing

The project includes comprehensive tests:

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test component interactions
- **End-to-End Tests**: Test complete scraping workflows

Run all tests:
```bash
mvn test
```

### Static Analysis

This project uses Checkstyle and SpotBugs to enforce code quality and find potential bugs.

- **Checkstyle**: Enforces a consistent coding style based on the Google Java Style Guide.
- **SpotBugs**: Performs static analysis to find common bug patterns.

To run the static analysis checks:
```bash
mvn verify
```

The build will fail if any Checkstyle or SpotBugs violations are found.

## Troubleshooting

### Common Issues

1. **"No data extracted"**
   - Check if the CSS selectors match the target website structure
   - Verify the website allows scraping (check robots.txt)
   - Ensure the website is accessible and returns valid HTML

2. **"Connection timeout"**
   - Increase the delay between requests (`--delay-ms`)
   - Check network connectivity
   - Verify the target URL is correct

3. **"robots.txt not found"**
   - This is normal for many sites
   - The scraper will assume crawling is allowed
   - Use `--no-respect-robots` if you want to ignore robots.txt entirely

### Debugging

Enable debug logging by setting the log level:
```bash
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
  -jar simple-web-scraper-1.0.0-jar-with-dependencies.jar \
  --url https://example.com
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests and static analysis checks pass
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues, questions, or contributions, please:
1. Check the troubleshooting section above
2. Search existing issues
3. Create a new issue with detailed information about your problem

## Changelog

### Version 1.0.0
- Initial release
- Robots.txt compliance
- CSV and JSONL output formats
- Rate limiting and error handling
- Comprehensive test suite
- Command-line interface

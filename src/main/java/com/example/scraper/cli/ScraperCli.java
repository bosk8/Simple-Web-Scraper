package com.example.scraper.cli;

import com.example.scraper.core.persistence.CSVWriter;
import com.example.scraper.core.persistence.JSONLWriter;
import com.example.scraper.model.DataSelectors;
import com.example.scraper.model.ScrapedData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command-line interface for the web scraper.
 */
public class ScraperCli {

  private static final Logger logger = LoggerFactory.getLogger(ScraperCli.class);

  /**
   * Main entry point for the CLI application.
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    try {
      ScraperConfig config = parseArguments(args);
      if (config == null) {
        printUsage();
        System.exit(1);
      }

      logger.info("Starting web scraper with configuration: {}", config);

      DataSelectors selectors = createDataSelectors(config);
      WebScraper scraper = new WebScraper(config.getUserAgent(), selectors);
      OutputWriter outputWriter = createOutputWriter(config);

      try {
        List<ScrapedData> allData;
        if (config.getUrls().size() == 1) {
          allData = scraper.scrapeUrl(config.getUrls().get(0), config.isRespectRobots());
        } else {
          CompletableFuture<List<ScrapedData>> future =
              scraper.scrapeUrlsAsync(config.getUrls(), config.isRespectRobots());
          allData = future.get();
        }

        if (!allData.isEmpty()) {
          outputWriter.writeData(allData);
          logger.info("Successfully scraped {} records", allData.size());
        } else {
          logger.warn("No data was scraped");
        }
      } finally {
        outputWriter.close();
      }

    } catch (Exception e) {
      logger.error("Error running scraper: {}", e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * Parses command-line arguments.
   *
   * @param args Command-line arguments
   * @return ScraperConfig or null if parsing failed
   */
  private static ScraperConfig parseArguments(String[] args) {
    ScraperConfig config = new ScraperConfig();

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      switch (arg) {
        case "--url":
          if (i + 1 < args.length) {
            config.addUrl(args[++i]);
          } else {
            logger.error("--url requires a URL argument");
            return null;
          }
          break;
        case "--max-pages":
          if (i + 1 < args.length) {
            try {
              config.setMaxPages(Integer.parseInt(args[++i]));
            } catch (NumberFormatException e) {
              logger.error("--max-pages requires a valid number");
              return null;
            }
          } else {
            logger.error("--max-pages requires a number argument");
            return null;
          }
          break;
        case "--output":
          if (i + 1 < args.length) {
            config.setOutputFile(args[++i]);
          } else {
            logger.error("--output requires a file path argument");
            return null;
          }
          break;
        case "--format":
          if (i + 1 < args.length) {
            String format = args[++i].toLowerCase();
            if (!format.equals("csv") && !format.equals("jsonl")) {
              logger.error("--format must be 'csv' or 'jsonl'");
              return null;
            }
            config.setOutputFormat(format);
          } else {
            logger.error("--format requires 'csv' or 'jsonl' argument");
            return null;
          }
          break;
        case "--delay-ms":
          if (i + 1 < args.length) {
            try {
              config.setDelayMs(Long.parseLong(args[++i]));
            } catch (NumberFormatException e) {
              logger.error("--delay-ms requires a valid number");
              return null;
            }
          } else {
            logger.error("--delay-ms requires a number argument");
            return null;
          }
          break;
        case "--user-agent":
          if (i + 1 < args.length) {
            config.setUserAgent(args[++i]);
          } else {
            logger.error("--user-agent requires a string argument");
            return null;
          }
          break;
        case "--respect-robots":
          config.setRespectRobots(true);
          break;
        case "--no-respect-robots":
          config.setRespectRobots(false);
          break;
        case "--help":
        case "-h":
          return null; // Will trigger usage display
        default:
          logger.error("Unknown argument: {}", arg);
          return null;
      }
    }

    if (config.getUrls().isEmpty()) {
      logger.error("At least one --url argument is required");
      return null;
    }

    return config;
  }

  /**
   * Creates data selectors based on configuration.
   *
   * @param config Scraper configuration
   * @return DataSelectors object
   */
  private static DataSelectors createDataSelectors(ScraperConfig config) {
    DataSelectors selectors = new DataSelectors();
    selectors.setContainerSelector("article, .item, .product, .post");
    selectors.setTitleSelector("h1, h2, h3, .title, .name");
    selectors.setDescriptionSelector("p, .description, .summary");
    selectors.setUrlSelector("a");
    selectors.setPriceSelector(".price, .cost");
    selectors.setImageSelector("img");
    return selectors;
  }

  /**
   * Creates the appropriate output writer based on configuration.
   *
   * @param config Scraper configuration
   * @return OutputWriter instance
   */
  private static OutputWriter createOutputWriter(ScraperConfig config) {
    String outputFile = config.getOutputFile();
    String format = config.getOutputFormat();

    if (format.equals("csv")) {
      return new OutputWriter(new CSVWriter(outputFile));
    } else {
      return new OutputWriter(new JSONLWriter(outputFile));
    }
  }

  /**
   * Prints usage information.
   */
  private static void printUsage() {
    System.out.println("Simple Web Scraper - Extract data from web pages");
    System.out.println();
    System.out.println("Usage: java -jar scraper.jar [options]");
    System.out.println();
    System.out.println("Required options:");
    System.out.println("  --url <url>              URL to scrape (can be specified multiple times)");
    System.out.println();
    System.out.println("Optional options:");
    System.out.println(
        "  --max-pages <number>     Maximum number of pages to scrape (default: unlimited)");
    System.out.println("  --output <file>          Output file path (default: output.csv)");
    System.out.println("  --format <format>        Output format: csv or jsonl (default: csv)");
    System.out.println("  --delay-ms <ms>          Minimum delay between requests (default: 1000)");
    System.out.println("  --user-agent <string>    Custom User-Agent string");
    System.out.println("  --respect-robots         Respect robots.txt rules (default: true)");
    System.out.println("  --no-respect-robots      Ignore robots.txt rules");
    System.out.println("  --help, -h               Show this help message");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  java -jar scraper.jar --url https://example.com --output data.csv");
    System.out.println(
        "  java -jar scraper.jar --url https://site1.com --url https://site2.com --format jsonl");
    System.out.println(
        "  java -jar scraper.jar --url https://example.com --delay-ms 2000 --no-respect-robots");
  }
}

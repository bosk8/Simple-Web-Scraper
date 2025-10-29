package com.example.scraper.cli;

import com.example.scraper.core.http.HttpFetcher;
import com.example.scraper.core.parser.HtmlParser;
import com.example.scraper.core.robots.RobotsTxtCompliance;
import com.example.scraper.model.DataSelectors;
import com.example.scraper.model.ScrapedData;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main scraper class that orchestrates the web scraping process.
 */
public class WebScraper {

  private static final Logger logger = LoggerFactory.getLogger(WebScraper.class);

  private final HttpFetcher httpFetcher;
  private final HtmlParser htmlParser;
  private final RobotsTxtCompliance robotsCompliance;
  private final DataSelectors selectors;

  /**
   * Constructs a new WebScraper.
   *
   * @param userAgent The User-Agent string to use
   * @param selectors The data selectors to use
   */
  public WebScraper(String userAgent, DataSelectors selectors) {
    this.selectors = new DataSelectors();
    this.selectors.setContainerSelector(selectors.getContainerSelector());
    this.selectors.setTitleSelector(selectors.getTitleSelector());
    this.selectors.setDescriptionSelector(selectors.getDescriptionSelector());
    this.selectors.setUrlSelector(selectors.getUrlSelector());
    this.selectors.setPriceSelector(selectors.getPriceSelector());
    this.selectors.setImageSelector(selectors.getImageSelector());

    HttpClient httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_2)
            .build();

    this.httpFetcher = new HttpFetcher(userAgent, Duration.ofSeconds(30));
    this.htmlParser = new HtmlParser();
    this.robotsCompliance = new RobotsTxtCompliance(httpClient);
  }

  /**
   * Scrapes a single URL and returns the extracted data.
   *
   * @param url           The URL to scrape
   * @param respectRobots Whether to respect robots.txt rules
   * @return List of scraped data
   */
  public List<ScrapedData> scrapeUrl(String url, boolean respectRobots) {
    try {
      if (respectRobots && !robotsCompliance.isUrlAllowed(url)) {
        logger.warn("URL not allowed by robots.txt: {}", url);
        return List.of();
      }

      long crawlDelay = respectRobots ? robotsCompliance.getCrawlDelay(url) : 1000;

      logger.info("Fetching URL: {}", url);
      HttpResponse<String> response = httpFetcher.fetch(url, crawlDelay);

      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        List<ScrapedData> data = htmlParser.parseHtml(response.body(), url, selectors);
        logger.info("Extracted {} records from URL: {}", data.size(), url);
        return data;
      } else {
        logger.warn("HTTP error {} for URL: {}", response.statusCode(), url);
        return List.of();
      }

    } catch (Exception e) {
      logger.error("Error scraping URL {}: {}", url, e.getMessage());
      return List.of();
    }
  }

  /**
   * Scrapes multiple URLs asynchronously.
   *
   * @param urls          List of URLs to scrape
   * @param respectRobots Whether to respect robots.txt rules
   * @return CompletableFuture containing list of all scraped data
   */
  public CompletableFuture<List<ScrapedData>> scrapeUrlsAsync(
      List<String> urls, boolean respectRobots) {
    List<CompletableFuture<List<ScrapedData>>> futures =
        urls.stream()
            .map(url -> CompletableFuture.supplyAsync(() -> scrapeUrl(url, respectRobots)))
            .toList();

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(
            v -> futures.stream().map(CompletableFuture::join).flatMap(List::stream).toList());
  }

  /**
   * Gets the User-Agent string used by this scraper.
   *
   * @return User-Agent string
   */
  public String getUserAgent() {
    return httpFetcher.getUserAgent();
  }

  /**
   * Gets the timeout duration used by this scraper.
   *
   * @return timeout duration
   */
  public Duration getTimeout() {
    return httpFetcher.getTimeout();
  }
}

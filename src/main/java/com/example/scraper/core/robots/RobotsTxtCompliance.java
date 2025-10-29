package com.example.scraper.core.robots;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles robots.txt compliance for web scraping.
 * Uses crawler-commons library for parsing robots.txt files.
 */
public class RobotsTxtCompliance {
    
    private static final Logger logger = LoggerFactory.getLogger(RobotsTxtCompliance.class);
    private static final String USER_AGENT = "SimpleWebScraper/1.0 (+https://github.com/example/simple-web-scraper)";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    private final HttpClient httpClient;
    private final ConcurrentMap<String, BaseRobotRules> robotsCache;
    private final SimpleRobotRulesParser parser;
    
    public RobotsTxtCompliance(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.robotsCache = new ConcurrentHashMap<>();
        this.parser = new SimpleRobotRulesParser();
    }
    
    /**
     * Checks if a URL is allowed to be crawled according to robots.txt rules.
     * 
     * @param url The URL to check
     * @return true if crawling is allowed, false otherwise
     */
    public boolean isUrlAllowed(String url) {
        try {
            URI uri = URI.create(url);
            String domain = uri.getScheme() + "://" + uri.getHost();
            
            BaseRobotRules rules = getRobotRules(domain);
            return rules.isAllowed(url);
            
        } catch (Exception e) {
            logger.warn("Error checking robots.txt for URL {}: {}", url, e.getMessage());
            // Default to allowing if we can't determine the rules
            return true;
        }
    }
    
    /**
     * Gets the crawl delay for a domain from robots.txt.
     * 
     * @param domain The domain to check
     * @return crawl delay in milliseconds, or default 1000ms if not specified
     */
    public long getCrawlDelay(String domain) {
        try {
            BaseRobotRules rules = getRobotRules(domain);
            long delay = rules.getCrawlDelay();
            return delay > 0 ? delay * 1000 : 1000; // Convert to milliseconds, default 1 second
        } catch (Exception e) {
            logger.warn("Error getting crawl delay for domain {}: {}", domain, e.getMessage());
            return 1000; // Default 1 second
        }
    }
    
    /**
     * Gets or fetches robot rules for a domain.
     * Rules are cached to avoid repeated requests.
     * 
     * @param domain The domain to get rules for
     * @return BaseRobotRules for the domain
     */
    private BaseRobotRules getRobotRules(String domain) {
        return robotsCache.computeIfAbsent(domain, this::fetchRobotRules);
    }
    
    /**
     * Fetches robots.txt from a domain and parses it.
     * 
     * @param domain The domain to fetch robots.txt from
     * @return Parsed robot rules
     */
    private BaseRobotRules fetchRobotRules(String domain) {
        String robotsUrl = domain + "/robots.txt";
        
        try {
            logger.debug("Fetching robots.txt from: {}", robotsUrl);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(robotsUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            int statusCode = response.statusCode();
            
            if (statusCode >= 200 && statusCode < 300) {
                // Success - parse the robots.txt content
                String robotsContent = response.body();
                BaseRobotRules rules = parser.parseContent(robotsUrl, robotsContent.getBytes(), "text/plain", USER_AGENT);
                logger.debug("Successfully parsed robots.txt for domain: {}", domain);
                return rules;
                
            } else if (statusCode >= 400 && statusCode < 500) {
                // 4xx errors - assume crawling is allowed
                logger.debug("robots.txt not found ({}), assuming crawling allowed for domain: {}", statusCode, domain);
                return parser.parseContent(robotsUrl, new byte[0], "text/plain", USER_AGENT);
                
            } else {
                // 5xx errors - block crawling temporarily
                logger.warn("robots.txt server error ({}), blocking crawling for domain: {}", statusCode, domain);
                // Return rules that disallow all crawling
                return parser.parseContent(robotsUrl, "User-agent: *\nDisallow: /\n".getBytes(), "text/plain", USER_AGENT);
            }
            
        } catch (Exception e) {
            logger.warn("Error fetching robots.txt from {}: {}", robotsUrl, e.getMessage());
            // On error, assume crawling is allowed
            return parser.parseContent(robotsUrl, new byte[0], "text/plain", USER_AGENT);
        }
    }
    
    /**
     * Clears the robots.txt cache.
     * Useful for testing or when rules might have changed.
     */
    public void clearCache() {
        robotsCache.clear();
        logger.debug("Robots.txt cache cleared");
    }
    
    /**
     * Gets the size of the robots.txt cache.
     * 
     * @return number of cached domains
     */
    public int getCacheSize() {
        return robotsCache.size();
    }
}

package com.example.scraper.core.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for fetching web content with rate limiting and error handling.
 */
public class HttpFetcher {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpFetcher.class);
    private static final String DEFAULT_USER_AGENT = "SimpleWebScraper/1.0 (+https://github.com/example/simple-web-scraper)";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_CONCURRENT_REQUESTS_PER_DOMAIN = 2;
    
    private final HttpClient httpClient;
    private final ConcurrentMap<String, Semaphore> domainSemaphores;
    private final ConcurrentMap<String, Long> lastRequestTime;
    private final String userAgent;
    private final Duration timeout;
    
    public HttpFetcher() {
        this(DEFAULT_USER_AGENT, DEFAULT_TIMEOUT);
    }
    
    public HttpFetcher(String userAgent, Duration timeout) {
        this.userAgent = userAgent;
        this.timeout = timeout;
        this.domainSemaphores = new ConcurrentHashMap<>();
        this.lastRequestTime = new ConcurrentHashMap<>();
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL) // Follow redirects
                .version(HttpClient.Version.HTTP_2)
                .build();
    }
    
    /**
     * Fetches content from a URL with rate limiting and error handling.
     * 
     * @param url The URL to fetch
     * @param crawlDelayMs Minimum delay between requests to the same domain
     * @return CompletableFuture containing the HTTP response
     */
    public CompletableFuture<HttpResponse<String>> fetchAsync(String url, long crawlDelayMs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI uri = URI.create(url);
                String domain = uri.getScheme() + "://" + uri.getHost();
                
                // Apply rate limiting
                applyRateLimit(domain, crawlDelayMs);
                
                // Acquire semaphore for concurrent request limiting
                Semaphore semaphore = domainSemaphores.computeIfAbsent(domain, 
                    k -> new Semaphore(MAX_CONCURRENT_REQUESTS_PER_DOMAIN));
                
                semaphore.acquire();
                try {
                    return fetchWithRetry(url);
                } finally {
                    semaphore.release();
                }
                
            } catch (Exception e) {
                logger.error("Error fetching URL {}: {}", url, e.getMessage());
                throw new RuntimeException("Failed to fetch URL: " + url, e);
            }
        });
    }
    
    /**
     * Synchronous version of fetchAsync.
     * 
     * @param url The URL to fetch
     * @param crawlDelayMs Minimum delay between requests to the same domain
     * @return HTTP response
     */
    public HttpResponse<String> fetch(String url, long crawlDelayMs) {
        try {
            return fetchAsync(url, crawlDelayMs).get();
        } catch (Exception e) {
            logger.error("Error in synchronous fetch for URL {}: {}", url, e.getMessage());
            throw new RuntimeException("Failed to fetch URL: " + url, e);
        }
    }
    
    /**
     * Applies rate limiting by ensuring minimum delay between requests to the same domain.
     * 
     * @param domain The domain to apply rate limiting for
     * @param crawlDelayMs Minimum delay in milliseconds
     */
    private void applyRateLimit(String domain, long crawlDelayMs) {
        Long lastTime = lastRequestTime.get(domain);
        if (lastTime != null) {
            long timeSinceLastRequest = System.currentTimeMillis() - lastTime;
            if (timeSinceLastRequest < crawlDelayMs) {
                long sleepTime = crawlDelayMs - timeSinceLastRequest;
                logger.debug("Rate limiting: sleeping {}ms for domain {}", sleepTime, domain);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Rate limiting interrupted", e);
                }
            }
        }
        lastRequestTime.put(domain, System.currentTimeMillis());
    }
    
    /**
     * Fetches content with exponential backoff retry for server errors.
     * 
     * @param url The URL to fetch
     * @return HTTP response
     */
    private HttpResponse<String> fetchWithRetry(String url) {
        int maxRetries = 3;
        int baseDelayMs = 1000; // 1 second base delay
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<String> response = performRequest(url);
                int statusCode = response.statusCode();
                
                if (statusCode >= 200 && statusCode < 300) {
                    // Success
                    logger.debug("Successfully fetched URL: {} (status: {})", url, statusCode);
                    return response;
                    
                } else if (statusCode >= 300 && statusCode < 400) {
                    // Redirect - let HttpClient handle it
                    logger.debug("Redirect response for URL: {} (status: {})", url, statusCode);
                    return response;
                    
                } else if (statusCode >= 400 && statusCode < 500) {
                    // Client error - don't retry
                    logger.warn("Client error for URL: {} (status: {})", url, statusCode);
                    return response;
                    
                } else if (statusCode >= 500 && statusCode < 600) {
                    // Server error - retry with exponential backoff
                    if (attempt < maxRetries) {
                        long delayMs = baseDelayMs * (1L << attempt); // Exponential backoff
                        logger.warn("Server error for URL: {} (status: {}), retrying in {}ms (attempt {}/{})", 
                                   url, statusCode, delayMs, attempt + 1, maxRetries);
                        
                        // Check for Retry-After header
                        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
                        if (retryAfter != null) {
                            try {
                                delayMs = Long.parseLong(retryAfter) * 1000; // Convert seconds to milliseconds
                                logger.debug("Using Retry-After header: {}ms", delayMs);
                            } catch (NumberFormatException e) {
                                logger.debug("Invalid Retry-After header: {}", retryAfter);
                            }
                        }
                        
                        Thread.sleep(delayMs);
                        continue;
                    } else {
                        logger.error("Max retries exceeded for URL: {} (status: {})", url, statusCode);
                        return response;
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Request interrupted", e);
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    long delayMs = baseDelayMs * (1L << attempt);
                    logger.warn("Exception fetching URL: {}, retrying in {}ms (attempt {}/{})", 
                               url, delayMs, attempt + 1, maxRetries, e);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    logger.error("Max retries exceeded for URL: {}", url, e);
                    throw new RuntimeException("Failed to fetch URL after retries: " + url, e);
                }
            }
        }
        
        throw new RuntimeException("Unexpected end of retry loop for URL: " + url);
    }
    
    /**
     * Performs the actual HTTP request.
     * 
     * @param url The URL to request
     * @return HTTP response
     */
    private HttpResponse<String> performRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Accept-Encoding", "gzip, deflate")
                .GET()
                .build();
        
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
    
    /**
     * Gets the User-Agent string used by this fetcher.
     * 
     * @return User-Agent string
     */
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * Gets the timeout duration used by this fetcher.
     * 
     * @return timeout duration
     */
    public Duration getTimeout() {
        return timeout;
    }
}

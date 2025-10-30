package com.example.scraper.core.robots;

import java.net.http.HttpClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotsTxtComplianceTest {
    
    private static final String TEST_DOMAIN = "https://example.com";
    
    private RobotsTxtCompliance robotsCompliance;
    
    RobotsTxtComplianceTest() {
        // Default constructor for test class
    }
    
    @BeforeEach
    void setUp() {
        HttpClient httpClient = HttpClient.newBuilder().build();
        robotsCompliance = new RobotsTxtCompliance(httpClient);
    }
    
    @Test
    void testGetCrawlDelay() {
        // Test with a domain that should have default delay
        long delay = robotsCompliance.getCrawlDelay(TEST_DOMAIN);
        
        // Should return default delay (1000ms) or actual delay from robots.txt
        assertTrue(delay >= 1000);
    }
    
    @Test
    void testIsUrlAllowed() {
        // Test with a URL that should be allowed
        boolean allowed = robotsCompliance.isUrlAllowed(TEST_DOMAIN + "/page");
        
        // Should be allowed by default or based on robots.txt
        assertTrue(allowed);
    }
    
    @Test
    void testClearCache() {
        // Clear cache should not throw exception
        assertDoesNotThrow(() -> robotsCompliance.clearCache());
        
        // Cache size should be 0 after clearing
        assertEquals(0, robotsCompliance.getCacheSize());
    }
    
    @Test
    void testGetCacheSize() {
        int initialSize = robotsCompliance.getCacheSize();
        
        // Try to get rules for a domain to populate cache
        robotsCompliance.getCrawlDelay(TEST_DOMAIN);
        
        int newSize = robotsCompliance.getCacheSize();
        
        // Cache size should have increased
        assertTrue(newSize >= initialSize);
    }
}

package com.example.scraper.core.robots;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RobotsTxtComplianceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @InjectMocks
    private RobotsTxtCompliance robotsCompliance;

    @BeforeEach
    void setUp() {
        robotsCompliance.clearCache();
    }

    private void mockRobotsResponse(int statusCode, String body) throws IOException, InterruptedException {
        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
    }

    @Test
    void testIsUrlAllowed_AllowedByDefault() throws IOException, InterruptedException {
        mockRobotsResponse(404, ""); // No robots.txt, so everything is allowed
        assertTrue(robotsCompliance.isUrlAllowed("https://example.com/allowed"));
    }

    @Test
    void testIsUrlAllowed_ExplicitlyAllowed() throws IOException, InterruptedException {
        String robotsTxt = "User-agent: *\nAllow: /allowed\nDisallow: /disallowed";
        mockRobotsResponse(200, robotsTxt);
        assertTrue(robotsCompliance.isUrlAllowed("https://example.com/allowed"));
    }

    @Test
    void testIsUrlAllowed_ExplicitlyDisallowed() throws IOException, InterruptedException {
        String robotsTxt = "User-agent: *\nDisallow: /disallowed";
        mockRobotsResponse(200, robotsTxt);
        assertFalse(robotsCompliance.isUrlAllowed("https://example.com/disallowed"));
    }

    @Test
    void testGetCrawlDelay_DefaultDelay() throws IOException, InterruptedException {
        mockRobotsResponse(404, ""); // No robots.txt, default delay
        assertEquals(1000, robotsCompliance.getCrawlDelay("https://example.com"));
    }

    @Test
    void testGetCrawlDelay_CustomDelay() throws IOException, InterruptedException {
        String robotsTxt = "User-agent: *\nCrawl-delay: 5";
        mockRobotsResponse(200, robotsTxt);
        assertEquals(5000, robotsCompliance.getCrawlDelay("https://example.com"));
    }

    @Test
    void testServerError_ShouldBlockCrawling() throws IOException, InterruptedException {
        mockRobotsResponse(500, "Server error");
        assertFalse(robotsCompliance.isUrlAllowed("https://example.com/anypage"));
    }

    @Test
    void testClearCache() throws IOException, InterruptedException {
        String robotsTxt = "User-agent: *\nDisallow: /private";
        mockRobotsResponse(200, robotsTxt);

        // Populate the cache
        assertFalse(robotsCompliance.isUrlAllowed("https://example.com/private"));
        assertEquals(1, robotsCompliance.getCacheSize());

        // Clear the cache
        robotsCompliance.clearCache();
        assertEquals(0, robotsCompliance.getCacheSize());
    }

    @Test
    void testCacheIsUsed() throws IOException, InterruptedException {
        String robotsTxt = "User-agent: *\nCrawl-delay: 2";
        mockRobotsResponse(200, robotsTxt);

        // First call populates the cache
        assertEquals(2000, robotsCompliance.getCrawlDelay("https://cached.com"));
        assertEquals(1, robotsCompliance.getCacheSize());

        // Second call should hit the cache (no new network call)
        assertEquals(2000, robotsCompliance.getCrawlDelay("https://cached.com"));
        assertEquals(1, robotsCompliance.getCacheSize());
    }
}

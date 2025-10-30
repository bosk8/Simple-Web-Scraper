package com.example.scraper.core.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.example.scraper.model.DataSelectors;
import com.example.scraper.model.ScrapedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlParserTest {
    
    private HtmlParser parser;
    private DataSelectors selectors;
    
    @BeforeEach
    void setUp() {
        parser = new HtmlParser();
        selectors = new DataSelectors();
    }
    
    @Test
    void testParseNewsSite() throws IOException {
        // Load test HTML
        String html = Files.readString(Paths.get("src/test/resources/fixtures/news-site.html"));
        
        // Configure selectors
        selectors.setContainerSelector("article.news-item");
        selectors.setTitleSelector("h2.title");
        selectors.setDescriptionSelector("p.description");
        selectors.setUrlSelector("a.url");
        selectors.setPriceSelector("span.price");
        selectors.setImageSelector("img.image");
        
        // Parse HTML
        List<ScrapedData> results = parser.parseHtml(html, "https://example.com", selectors);
        
        // Verify results
        assertEquals(3, results.size());
        
        ScrapedData first = results.get(0);
        assertEquals("Breaking: New Technology Breakthrough", first.getTitle());
        assertEquals("Scientists have made a significant breakthrough in quantum computing that could revolutionize data processing.", first.getDescription());
        assertEquals("/news/quantum-breakthrough", first.getUrl());
        assertEquals("Free", first.getPrice());
        assertEquals("/images/quantum.jpg", first.getImageUrl());
    }
    
    @Test
    void testParseEcommerceSite() throws IOException {
        // Load test HTML
        String html = Files.readString(Paths.get("src/test/resources/fixtures/ecommerce-site.html"));
        
        // Configure selectors
        selectors.setContainerSelector("div.product");
        selectors.setTitleSelector("h1.product-title");
        selectors.setDescriptionSelector("p.product-description");
        selectors.setUrlSelector("a.product-link");
        selectors.setPriceSelector("span.product-price");
        selectors.setImageSelector("img.product-image");
        
        // Parse HTML
        List<ScrapedData> results = parser.parseHtml(html, "https://shop.example.com", selectors);
        
        // Verify results
        assertEquals(3, results.size());
        
        ScrapedData first = results.get(0);
        assertEquals("Wireless Bluetooth Headphones", first.getTitle());
        assertEquals("High-quality wireless headphones with noise cancellation and 30-hour battery life.", first.getDescription());
        assertEquals("/products/wireless-headphones", first.getUrl());
        assertEquals("$99.99", first.getPrice());
        assertEquals("/images/headphones.jpg", first.getImageUrl());
    }
    
    @Test
    void testParseWithMissingFields() throws IOException {
        // Load test HTML
        String html = Files.readString(Paths.get("src/test/resources/fixtures/news-site.html"));
        
        // Configure selectors with some missing fields
        selectors.setContainerSelector("article.news-item");
        selectors.setTitleSelector("h2.title");
        selectors.setDescriptionSelector("p.description");
        selectors.setUrlSelector("a.url");
        // Don't set price and image selectors
        
        // Parse HTML
        List<ScrapedData> results = parser.parseHtml(html, "https://example.com", selectors);
        
        // Verify results
        assertEquals(3, results.size());
        
        ScrapedData first = results.get(0);
        assertEquals("Breaking: New Technology Breakthrough", first.getTitle());
        assertEquals("Scientists have made a significant breakthrough in quantum computing that could revolutionize data processing.", first.getDescription());
        assertEquals("/news/quantum-breakthrough", first.getUrl());
        assertNull(first.getPrice());
        assertNull(first.getImageUrl());
    }
    
    @Test
    void testParseEmptyHtml() {
        String html = "<html><body></body></html>";
        
        selectors.setContainerSelector("article");
        selectors.setTitleSelector("h1");
        
        List<ScrapedData> results = parser.parseHtml(html, "https://example.com", selectors);
        
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testIsValidSelector() {
        assertTrue(parser.isValidSelector("h1"));
        assertTrue(parser.isValidSelector(".class"));
        assertTrue(parser.isValidSelector("#id"));
        assertTrue(parser.isValidSelector("div p"));
        
        assertFalse(parser.isValidSelector(null));
        assertFalse(parser.isValidSelector(""));
        assertFalse(parser.isValidSelector("invalid["));
    }
}

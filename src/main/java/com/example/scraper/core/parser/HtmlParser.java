package com.example.scraper.core.parser;

import com.example.scraper.model.ScrapedData;
import com.example.scraper.model.DataSelectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HTML parser using jsoup for extracting data with CSS selectors.
 */
public class HtmlParser {
    
    private static final Logger logger = LoggerFactory.getLogger(HtmlParser.class);
    
    /**
     * Parses HTML content and extracts data using CSS selectors.
     * 
     * @param htmlContent The HTML content to parse
     * @param baseUrl The base URL for resolving relative links
     * @param selectors CSS selectors for data extraction
     * @return List of scraped data
     */
    public List<ScrapedData> parseHtml(String htmlContent, String baseUrl, DataSelectors selectors) {
        try {
            Document document = Jsoup.parse(htmlContent, baseUrl);
            return extractData(document, selectors);
        } catch (Exception e) {
            logger.error("Error parsing HTML content: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Extracts data from a jsoup Document using CSS selectors.
     * 
     * @param document The jsoup Document
     * @param selectors CSS selectors for data extraction
     * @return List of scraped data
     */
    private List<ScrapedData> extractData(Document document, DataSelectors selectors) {
        List<ScrapedData> results = new ArrayList<>();
        
        try {
            // Find the container elements (e.g., article, .item, etc.)
            Elements containers = document.select(selectors.getContainerSelector());
            
            if (containers.isEmpty()) {
                logger.warn("No containers found with selector: {}", selectors.getContainerSelector());
                return results;
            }
            
            logger.debug("Found {} containers with selector: {}", containers.size(), selectors.getContainerSelector());
            
            for (Element container : containers) {
                try {
                    ScrapedData data = extractDataFromContainer(container, selectors);
                    if (data != null) {
                        results.add(data);
                    }
                } catch (Exception e) {
                    logger.warn("Error extracting data from container: {}", e.getMessage());
                    // Continue with next container
                }
            }
            
        } catch (Exception e) {
            logger.error("Error extracting data from document: {}", e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Extracts data from a single container element.
     * 
     * @param container The container element
     * @param selectors CSS selectors for data extraction
     * @return ScrapedData object or null if extraction fails
     */
    private ScrapedData extractDataFromContainer(Element container, DataSelectors selectors) {
        ScrapedData data = new ScrapedData();
        
        // Extract title
        Optional<String> title = extractText(container, selectors.getTitleSelector());
        if (title.isPresent()) {
            data.setTitle(normalizeText(title.get()));
        }
        
        // Extract description
        Optional<String> description = extractText(container, selectors.getDescriptionSelector());
        if (description.isPresent()) {
            data.setDescription(normalizeText(description.get()));
        }
        
        // Extract URL
        Optional<String> url = extractUrl(container, selectors.getUrlSelector());
        if (url.isPresent()) {
            data.setUrl(normalizeUrl(url.get()));
        }
        
        // Extract price if selector is provided
        if (selectors.getPriceSelector() != null) {
            Optional<String> price = extractText(container, selectors.getPriceSelector());
            if (price.isPresent()) {
                data.setPrice(normalizeText(price.get()));
            }
        }
        
        // Extract image URL if selector is provided
        if (selectors.getImageSelector() != null) {
            Optional<String> imageUrl = extractUrl(container, selectors.getImageSelector());
            if (imageUrl.isPresent()) {
                data.setImageUrl(normalizeUrl(imageUrl.get()));
            }
        }
        
        // Only return data if we have at least a title or URL
        if (data.getTitle() != null || data.getUrl() != null) {
            return data;
        }
        
        return null;
    }
    
    /**
     * Extracts text content from an element using a CSS selector.
     * 
     * @param container The container element
     * @param selector CSS selector
     * @return Optional containing the extracted text
     */
    private Optional<String> extractText(Element container, String selector) {
        if (selector == null || selector.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            Element element = container.selectFirst(selector);
            if (element != null) {
                String text = element.text();
                if (text != null && !text.trim().isEmpty()) {
                    return Optional.of(text);
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting text with selector '{}': {}", selector, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Extracts URL from an element using a CSS selector.
     * 
     * @param container The container element
     * @param selector CSS selector
     * @return Optional containing the extracted URL
     */
    private Optional<String> extractUrl(Element container, String selector) {
        if (selector == null || selector.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            Element element = container.selectFirst(selector);
            if (element != null) {
                String url = element.attr("href");
                if (url == null || url.trim().isEmpty()) {
                    url = element.attr("src");
                }
                if (url != null && !url.trim().isEmpty()) {
                    return Optional.of(url);
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting URL with selector '{}': {}", selector, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Normalizes text by trimming whitespace and handling empty strings.
     * 
     * @param text The text to normalize
     * @return Normalized text or null if empty
     */
    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }
    
    /**
     * Normalizes URL by resolving relative URLs and validating format.
     * 
     * @param url The URL to normalize
     * @return Normalized URL or null if invalid
     */
    private String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Handle relative URLs
            if (url.startsWith("//")) {
                url = "https:" + url;
            } else if (url.startsWith("/")) {
                // This would need the base URL, but for now just return as-is
                return url;
            }
            
            // Validate URL format
            URI uri = new URI(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            
            return url.trim();
            
        } catch (URISyntaxException e) {
            logger.debug("Invalid URL format: {}", url);
            return null;
        }
    }
    
    /**
     * Gets the base URL from a Document.
     * 
     * @param document The jsoup Document
     * @return Base URL or null if not available
     */
    public String getBaseUrl(Document document) {
        return document.baseUri();
    }
    
    /**
     * Validates that a CSS selector is valid.
     * 
     * @param selector The CSS selector to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidSelector(String selector) {
        if (selector == null || selector.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Try to parse the selector with jsoup
            Document testDoc = Jsoup.parse("<div></div>");
            testDoc.select(selector);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

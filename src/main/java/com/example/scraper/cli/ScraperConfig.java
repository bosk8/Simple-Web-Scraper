package com.example.scraper.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for the web scraper CLI.
 */
public class ScraperConfig {
    
    private final List<String> urls = new ArrayList<>();
    private int maxPages = Integer.MAX_VALUE;
    private String outputFile = "output.csv";
    private String outputFormat = "csv";
    private long delayMs = 1000;
    private String userAgent = "SimpleWebScraper/1.0 (+https://github.com/example/simple-web-scraper)";
    private boolean respectRobots = true;
    
    // Getters and setters
    public List<String> getUrls() {
        return urls;
    }
    
    public void addUrl(String url) {
        this.urls.add(url);
    }
    
    public int getMaxPages() {
        return maxPages;
    }
    
    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }
    
    public String getOutputFile() {
        return outputFile;
    }
    
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }
    
    public String getOutputFormat() {
        return outputFormat;
    }
    
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
    
    public long getDelayMs() {
        return delayMs;
    }
    
    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public boolean isRespectRobots() {
        return respectRobots;
    }
    
    public void setRespectRobots(boolean respectRobots) {
        this.respectRobots = respectRobots;
    }
    
    @Override
    public String toString() {
        return "ScraperConfig{" +
                "urls=" + urls +
                ", maxPages=" + maxPages +
                ", outputFile='" + outputFile + '\'' +
                ", outputFormat='" + outputFormat + '\'' +
                ", delayMs=" + delayMs +
                ", userAgent='" + userAgent + '\'' +
                ", respectRobots=" + respectRobots +
                '}';
    }
}

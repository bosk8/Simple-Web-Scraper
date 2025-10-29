package com.example.scraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for CSS selectors used in data extraction.
 */
public class DataSelectors {
    
    @JsonProperty("container_selector")
    private String containerSelector;
    
    @JsonProperty("title_selector")
    private String titleSelector;
    
    @JsonProperty("description_selector")
    private String descriptionSelector;
    
    @JsonProperty("url_selector")
    private String urlSelector;
    
    @JsonProperty("price_selector")
    private String priceSelector;
    
    @JsonProperty("image_selector")
    private String imageSelector;
    
    public DataSelectors() {
        // Default constructor for Jackson
    }
    
    public DataSelectors(String containerSelector, String titleSelector, 
                        String descriptionSelector, String urlSelector) {
        this.containerSelector = containerSelector;
        this.titleSelector = titleSelector;
        this.descriptionSelector = descriptionSelector;
        this.urlSelector = urlSelector;
    }
    
    // Getters and setters
    public String getContainerSelector() {
        return containerSelector;
    }
    
    public void setContainerSelector(String containerSelector) {
        this.containerSelector = containerSelector;
    }
    
    public String getTitleSelector() {
        return titleSelector;
    }
    
    public void setTitleSelector(String titleSelector) {
        this.titleSelector = titleSelector;
    }
    
    public String getDescriptionSelector() {
        return descriptionSelector;
    }
    
    public void setDescriptionSelector(String descriptionSelector) {
        this.descriptionSelector = descriptionSelector;
    }
    
    public String getUrlSelector() {
        return urlSelector;
    }
    
    public void setUrlSelector(String urlSelector) {
        this.urlSelector = urlSelector;
    }
    
    public String getPriceSelector() {
        return priceSelector;
    }
    
    public void setPriceSelector(String priceSelector) {
        this.priceSelector = priceSelector;
    }
    
    public String getImageSelector() {
        return imageSelector;
    }
    
    public void setImageSelector(String imageSelector) {
        this.imageSelector = imageSelector;
    }
    
    @Override
    public String toString() {
        return "DataSelectors{" +
                "containerSelector='" + containerSelector + '\'' +
                ", titleSelector='" + titleSelector + '\'' +
                ", descriptionSelector='" + descriptionSelector + '\'' +
                ", urlSelector='" + urlSelector + '\'' +
                ", priceSelector='" + priceSelector + '\'' +
                ", imageSelector='" + imageSelector + '\'' +
                '}';
    }
}

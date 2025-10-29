package com.example.scraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Represents scraped data from a web page.
 */
public class ScrapedData {

  @JsonProperty("title")
  private String title;

  @JsonProperty("description")
  private String description;

  @JsonProperty("url")
  private String url;

  @JsonProperty("price")
  private String price;

  @JsonProperty("image_url")
  private String imageUrl;

  public ScrapedData() {
    // Default constructor for Jackson
  }

  /**
   * Constructs a new ScrapedData object.
   *
   * @param title       The title of the scraped item
   * @param description The description of the scraped item
   * @param url         The URL of the scraped item
   */
  public ScrapedData(String title, String description, String url) {
    this.title = title;
    this.description = description;
    this.url = url;
  }

  // Getters and setters
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getPrice() {
    return price;
  }

  public void setPrice(String price) {
    this.price = price;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  @Override
  public String toString() {
    return "ScrapedData{"
        + "title='" + title + '\''
        + ", description='" + description + '\''
        + ", url='" + url + '\''
        + ", price='" + price + '\''
        + ", imageUrl='" + imageUrl + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScrapedData that = (ScrapedData) o;
    return Objects.equals(title, that.title)
        && Objects.equals(description, that.description)
        && Objects.equals(url, that.url)
        && Objects.equals(price, that.price)
        && Objects.equals(imageUrl, that.imageUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, description, url, price, imageUrl);
  }
}

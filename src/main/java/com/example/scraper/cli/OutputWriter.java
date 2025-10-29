package com.example.scraper.cli;

import com.example.scraper.core.persistence.CSVWriter;
import com.example.scraper.core.persistence.JSONLWriter;
import com.example.scraper.model.ScrapedData;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper class for output writers to provide a unified interface.
 */
public class OutputWriter {

  private static final Logger logger = LoggerFactory.getLogger(OutputWriter.class);

  private final CSVWriter csvWriter;
  private final JSONLWriter jsonlWriter;

  /**
   * Constructs a new OutputWriter for CSV output.
   *
   * @param csvWriter The CSVWriter to use
   */
  public OutputWriter(CSVWriter csvWriter) {
    this.csvWriter = new CSVWriter(csvWriter.getCurrentFile().getPath(), true);
    this.jsonlWriter = null;
  }

  /**
   * Constructs a new OutputWriter for JSONL output.
   *
   * @param jsonlWriter The JSONLWriter to use
   */
  public OutputWriter(JSONLWriter jsonlWriter) {
    this.csvWriter = null;
    this.jsonlWriter = new JSONLWriter(jsonlWriter.getCurrentFile().getPath(), true);
  }

  /**
   * Writes a list of scraped data.
   *
   * @param dataList List of scraped data
   */
  public void writeData(List<ScrapedData> dataList) {
    if (csvWriter != null) {
      csvWriter.writeData(dataList);
    } else if (jsonlWriter != null) {
      jsonlWriter.writeData(dataList);
    }
  }

  /**
   * Writes a single scraped data record.
   *
   * @param data Scraped data record
   */
  public void writeData(ScrapedData data) {
    if (csvWriter != null) {
      csvWriter.writeData(data);
    } else if (jsonlWriter != null) {
      jsonlWriter.writeData(data);
    }
  }

  /**
   * Closes the output writer.
   */
  public void close() {
    if (csvWriter != null) {
      csvWriter.close();
    } else if (jsonlWriter != null) {
      jsonlWriter.close();
    }
  }
}

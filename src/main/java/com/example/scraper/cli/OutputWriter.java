package com.example.scraper.cli;

import com.example.scraper.core.persistence.CSVWriter;
import com.example.scraper.core.persistence.JSONLWriter;
import com.example.scraper.model.ScrapedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Wrapper class for output writers to provide a unified interface.
 */
public class OutputWriter {
    
    private static final Logger logger = LoggerFactory.getLogger(OutputWriter.class);
    
    private final CSVWriter csvWriter;
    private final JSONLWriter jsonlWriter;
    
    public OutputWriter(CSVWriter csvWriter) {
        this.csvWriter = csvWriter;
        this.jsonlWriter = null;
    }
    
    public OutputWriter(JSONLWriter jsonlWriter) {
        this.csvWriter = null;
        this.jsonlWriter = jsonlWriter;
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

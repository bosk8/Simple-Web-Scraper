package com.example.scraper.core.persistence;

import com.example.scraper.model.ScrapedData;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Writes scraped data to CSV files using Jackson CSV module.
 */
public class CSVWriter {
    
    private static final Logger logger = LoggerFactory.getLogger(CSVWriter.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String FILE_ENCODING = "UTF-8";
    
    private final CsvMapper csvMapper;
    private final CsvSchema schema;
    private final ObjectWriter writer;
    private final String outputPath;
    private final boolean appendMode;
    
    private File currentFile;
    private FileWriter fileWriter;
    private boolean headerWritten = false;
    private int fileCounter = 0;
    
    public CSVWriter(String outputPath) {
        this(outputPath, false);
    }
    
    public CSVWriter(String outputPath, boolean appendMode) {
        this.outputPath = outputPath;
        this.appendMode = appendMode;
        
        this.csvMapper = new CsvMapper();
        this.schema = csvMapper.schemaFor(ScrapedData.class)
                .withHeader()
                .withColumnSeparator(',')
                .withQuoteChar('"')
                .withLineSeparator("\n");
        
        this.writer = csvMapper.writer(schema);
        
        initializeFile();
    }
    
    /**
     * Initializes the output file.
     */
    private void initializeFile() {
        try {
            Path path = Paths.get(outputPath);
            Path parentDir = path.getParent();
            
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            
            // Determine the actual file to use
            if (appendMode && Files.exists(path)) {
                currentFile = path.toFile();
                fileWriter = new FileWriter(currentFile, true); // Append mode
                headerWritten = true; // Assume header already exists
                logger.debug("Opened existing file for appending: {}", currentFile.getAbsolutePath());
            } else {
                currentFile = path.toFile();
                fileWriter = new FileWriter(currentFile, false); // Overwrite mode
                headerWritten = false;
                logger.debug("Created new file: {}", currentFile.getAbsolutePath());
            }
            
        } catch (IOException e) {
            logger.error("Error initializing CSV file: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize CSV file", e);
        }
    }
    
    /**
     * Writes a list of scraped data to CSV.
     * 
     * @param dataList List of scraped data
     */
    public void writeData(List<ScrapedData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        
        try {
            // Check if we need to rotate the file
            if (shouldRotateFile()) {
                rotateFile();
            }
            
            // Write header if needed
            if (!headerWritten) {
                writeHeader();
                headerWritten = true;
            }
            
            // Write data
            for (ScrapedData data : dataList) {
                String csvLine = writer.writeValueAsString(data);
                fileWriter.write(csvLine);
            }
            
            fileWriter.flush();
            logger.debug("Wrote {} records to CSV file", dataList.size());
            
        } catch (IOException e) {
            logger.error("Error writing CSV data: {}", e.getMessage());
            throw new RuntimeException("Failed to write CSV data", e);
        }
    }
    
    /**
     * Writes a single scraped data record to CSV.
     * 
     * @param data Scraped data record
     */
    public void writeData(ScrapedData data) {
        if (data == null) {
            return;
        }
        
        try {
            // Check if we need to rotate the file
            if (shouldRotateFile()) {
                rotateFile();
            }
            
            // Write header if needed
            if (!headerWritten) {
                writeHeader();
                headerWritten = true;
            }
            
            // Write data
            String csvLine = writer.writeValueAsString(data);
            fileWriter.write(csvLine);
            fileWriter.flush();
            
        } catch (IOException e) {
            logger.error("Error writing CSV data: {}", e.getMessage());
            throw new RuntimeException("Failed to write CSV data", e);
        }
    }
    
    /**
     * Writes the CSV header.
     */
    private void writeHeader() throws IOException {
        String header = "title,description,url,price,image_url\n";
        fileWriter.write(header);
        fileWriter.flush();
    }
    
    /**
     * Checks if the current file should be rotated due to size.
     * 
     * @return true if file should be rotated
     */
    private boolean shouldRotateFile() {
        return currentFile != null && currentFile.length() > MAX_FILE_SIZE;
    }
    
    /**
     * Rotates the current file to a new one with incremented name.
     */
    private void rotateFile() {
        try {
            // Close current file
            if (fileWriter != null) {
                fileWriter.close();
            }
            
            // Create new file with incremented name
            fileCounter++;
            String baseName = outputPath.substring(0, outputPath.lastIndexOf('.'));
            String extension = outputPath.substring(outputPath.lastIndexOf('.'));
            String newFileName = baseName + "_" + fileCounter + extension;
            
            currentFile = new File(newFileName);
            fileWriter = new FileWriter(currentFile, false);
            headerWritten = false; // Reset header flag for new file
            
            logger.info("Rotated CSV file to: {}", currentFile.getAbsolutePath());
            
        } catch (IOException e) {
            logger.error("Error rotating CSV file: {}", e.getMessage());
            throw new RuntimeException("Failed to rotate CSV file", e);
        }
    }
    
    /**
     * Closes the CSV writer and releases resources.
     */
    public void close() {
        try {
            if (fileWriter != null) {
                fileWriter.close();
            }
            logger.debug("CSV writer closed");
        } catch (IOException e) {
            logger.error("Error closing CSV writer: {}", e.getMessage());
        }
    }
    
    /**
     * Gets the current file being written to.
     * 
     * @return Current file
     */
    public File getCurrentFile() {
        return currentFile;
    }
    
    /**
     * Gets the number of files created (including rotations).
     * 
     * @return Number of files created
     */
    public int getFileCount() {
        return fileCounter + 1;
    }
}

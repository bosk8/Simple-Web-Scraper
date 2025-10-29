package com.example.scraper.core.persistence;

import com.example.scraper.model.ScrapedData;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Writes scraped data to JSONL (JSON Lines) files for streaming output.
 */
public class JSONLWriter {
    
    private static final Logger logger = LoggerFactory.getLogger(JSONLWriter.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String FILE_ENCODING = "UTF-8";
    
    private final ObjectMapper objectMapper;
    private final String outputPath;
    private final boolean appendMode;
    
    private File currentFile;
    private FileWriter fileWriter;
    private int fileCounter = 0;
    
    public JSONLWriter(String outputPath) {
        this(outputPath, false);
    }
    
    public JSONLWriter(String outputPath, boolean appendMode) {
        this.outputPath = outputPath;
        this.appendMode = appendMode;
        this.objectMapper = new ObjectMapper();
        
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
                logger.debug("Opened existing file for appending: {}", currentFile.getAbsolutePath());
            } else {
                currentFile = path.toFile();
                fileWriter = new FileWriter(currentFile, false); // Overwrite mode
                logger.debug("Created new file: {}", currentFile.getAbsolutePath());
            }
            
        } catch (IOException e) {
            logger.error("Error initializing JSONL file: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize JSONL file", e);
        }
    }
    
    /**
     * Writes a list of scraped data to JSONL.
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
            
            // Write each record as a separate JSON line
            for (ScrapedData data : dataList) {
                String jsonLine = objectMapper.writeValueAsString(data);
                fileWriter.write(jsonLine);
                fileWriter.write("\n"); // JSONL format: one JSON object per line
            }
            
            fileWriter.flush();
            logger.debug("Wrote {} records to JSONL file", dataList.size());
            
        } catch (IOException e) {
            logger.error("Error writing JSONL data: {}", e.getMessage());
            throw new RuntimeException("Failed to write JSONL data", e);
        }
    }
    
    /**
     * Writes a single scraped data record to JSONL.
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
            
            // Write record as JSON line
            String jsonLine = objectMapper.writeValueAsString(data);
            fileWriter.write(jsonLine);
            fileWriter.write("\n"); // JSONL format: one JSON object per line
            fileWriter.flush();
            
        } catch (IOException e) {
            logger.error("Error writing JSONL data: {}", e.getMessage());
            throw new RuntimeException("Failed to write JSONL data", e);
        }
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
            
            logger.info("Rotated JSONL file to: {}", currentFile.getAbsolutePath());
            
        } catch (IOException e) {
            logger.error("Error rotating JSONL file: {}", e.getMessage());
            throw new RuntimeException("Failed to rotate JSONL file", e);
        }
    }
    
    /**
     * Closes the JSONL writer and releases resources.
     */
    public void close() {
        try {
            if (fileWriter != null) {
                fileWriter.close();
            }
            logger.debug("JSONL writer closed");
        } catch (IOException e) {
            logger.error("Error closing JSONL writer: {}", e.getMessage());
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

package com.example.scraper.core.persistence;

import com.example.scraper.model.ScrapedData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes scraped data to JSONL (JSON Lines) files for streaming output.
 */
public class JSONLWriter {

  private static final Logger logger = LoggerFactory.getLogger(JSONLWriter.class);
  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

  private final ObjectMapper objectMapper;
  private final String outputPath;
  private final boolean appendMode;

  private File currentFile;
  private BufferedWriter fileWriter;
  private int fileCounter = 0;

  /**
   * Constructs a new JSONLWriter.
   *
   * @param outputPath The path to the output file
   */
  public JSONLWriter(String outputPath) {
    this(outputPath, false);
  }

  /**
   * Constructs a new JSONLWriter.
   *
   * @param outputPath The path to the output file
   * @param appendMode Whether to append to an existing file
   */
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
      if (appendMode && Files.exists(path)) {
        currentFile = path.toFile();
        fileWriter =
            Files.newBufferedWriter(
                currentFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        logger.debug("Opened existing file for appending: {}", currentFile.getAbsolutePath());
      } else {
        currentFile = path.toFile();
        fileWriter =
            Files.newBufferedWriter(
                currentFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
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
      if (shouldRotateFile()) {
        rotateFile();
      }
      for (ScrapedData data : dataList) {
        String jsonLine = objectMapper.writeValueAsString(data);
        fileWriter.write(jsonLine);
        fileWriter.write("\n");
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
      if (shouldRotateFile()) {
        rotateFile();
      }
      String jsonLine = objectMapper.writeValueAsString(data);
      fileWriter.write(jsonLine);
      fileWriter.write("\n");
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
   * Rotates the current file to a new one with an incremented name.
   */
  private void rotateFile() {
    try {
      fileCounter++;
      String baseName = outputPath.substring(0, outputPath.lastIndexOf('.'));
      String extension = outputPath.substring(outputPath.lastIndexOf('.'));
      String newFileName = baseName + "_" + fileCounter + extension;
      currentFile = new File(newFileName);
      fileWriter =
          Files.newBufferedWriter(
              currentFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
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
    return new File(currentFile.getPath());
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

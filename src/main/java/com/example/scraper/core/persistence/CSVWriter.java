package com.example.scraper.core.persistence;

import com.example.scraper.model.ScrapedData;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
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
 * Writes scraped data to CSV files using Jackson CSV module.
 */
public class CSVWriter {

  private static final Logger logger = LoggerFactory.getLogger(CSVWriter.class);
  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

  private final CsvMapper csvMapper;
  private final CsvSchema schema;
  private final ObjectWriter writer;
  private final String outputPath;
  private final boolean appendMode;

  private File currentFile;
  private BufferedWriter fileWriter;
  private boolean headerWritten = false;
  private int fileCounter = 0;

  /**
   * Constructs a new CSVWriter.
   *
   * @param outputPath The path to the output file
   */
  public CSVWriter(String outputPath) {
    this(outputPath, false);
  }

  /**
   * Constructs a new CSVWriter.
   *
   * @param outputPath The path to the output file
   * @param appendMode Whether to append to an existing file
   */
  public CSVWriter(String outputPath, boolean appendMode) {
    this.outputPath = outputPath;
    this.appendMode = appendMode;

    this.csvMapper = new CsvMapper();
    this.schema =
        csvMapper
            .schemaFor(ScrapedData.class)
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
      if (appendMode && Files.exists(path)) {
        currentFile = path.toFile();
        fileWriter =
            Files.newBufferedWriter(
                currentFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        headerWritten = true; // Assume header already exists
        logger.debug("Opened existing file for appending: {}", currentFile.getAbsolutePath());
      } else {
        currentFile = path.toFile();
        fileWriter =
            Files.newBufferedWriter(
                currentFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
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
      if (shouldRotateFile()) {
        rotateFile();
      }
      if (!headerWritten) {
        writeHeader();
        headerWritten = true;
      }
      for (ScrapedData data : dataList) {
        String csvLine = this.writer.writeValueAsString(data);
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
      if (shouldRotateFile()) {
        rotateFile();
      }
      if (!headerWritten) {
        writeHeader();
        headerWritten = true;
      }
      String csvLine = this.writer.writeValueAsString(data);
      fileWriter.write(csvLine);
      fileWriter.flush();
    } catch (IOException e) {
      logger.error("Error writing CSV data: {}", e.getMessage());
      throw new RuntimeException("Failed to write CSV data", e);
    }
  }

  /**
   * Writes the CSV header.
   *
   * @throws IOException if an error occurs while writing
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

/**
 * 
 */
package com.sirma.itt.emf.bam.csv;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;

import com.sirma.itt.commons.utils.string.StringUtils;

/**
 * Class containing functionality for writing data into a CSV file.
 * 
 * @author Mihail Radkov
 */
// TODO: Include column headers.
public class CSVLogger {

	/** Logger used for displaying messages related to this class. */
	private final Logger logger = LoggerFactory.getLogger(CSVLogger.class);

	/** The CSV writer used for writing data. */
	private CSVWriter csvWriter;

	/**
	 * Class constructor. Initializes the writer and checks if the provided file path points to an
	 * existing file. If not then creates a new file containing the specified separator.
	 * 
	 * @param logPath
	 *            the path for the CSV log file
	 * @param separator
	 *            the separator used in the CSV log file
	 */
	public CSVLogger(String logPath, char separator) {
		FileWriter fileWriter;
		try {
			if (!isFileExisting(logPath)) {
				fileWriter = new FileWriter(logPath, true);
				fileWriter.write("sep=" + separator + System.getProperty("line.separator"));
				fileWriter.flush();
			} else {
				fileWriter = new FileWriter(logPath, true);
			}
		} catch (IOException e) {
			logger.error("IO exception occured while opening/creating the CSV file.");
			throw new IllegalArgumentException("Wrong file path. IO exception occured.", e);
		}
		csvWriter = new CSVWriter(fileWriter, separator);
		logger.info("Initialized.");
	}

	/**
	 * Writes a message into the CSV file.
	 * 
	 * @param content
	 *            the message
	 */
	public void writeMessage(String[] content) {
		csvWriter.writeNext(content);
		try {
			csvWriter.flush();
		} catch (IOException e) {
			logger.error("Cannot write to the CSV file.", e);
		}
	}

	/**
	 * Checks if a file is existing at the provided path.
	 * 
	 * @param path
	 *            the provided file path
	 * @return true if the file exists and false if not or the file path is null
	 */
	public boolean isFileExisting(String path) {
		if (StringUtils.isNullOrEmpty(path)) {
			throw new IllegalArgumentException("Cannot pass a null or empty value for file path.");
		} else {
			return Files.exists(Paths.get(path));
		}
	}

	/**
	 * Closes the CSV file writer.
	 * 
	 * @throws IOException
	 *             if input/output related problem occurs
	 */
	public void closeCSVLogger() throws IOException {
		csvWriter.close();
		logger.info("Writer closed.");
	}

}

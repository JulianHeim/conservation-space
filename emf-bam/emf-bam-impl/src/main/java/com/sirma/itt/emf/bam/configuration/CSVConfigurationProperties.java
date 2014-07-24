/**
 * 
 */
package com.sirma.itt.emf.bam.configuration;

import com.sirma.itt.emf.configuration.Configuration;
import com.sirma.itt.emf.util.Documentation;

/**
 * Configuration properties for CSV file logging.
 * 
 * @author Mihail Radkov
 */
@Documentation("Configuration for CSV file logging.")
public interface CSVConfigurationProperties extends Configuration {

	/** Property for enabling logging events into a CSV file. */
	@Documentation("Property for enabling logging events into a CSV file.")
	String CSV_ENABLED = "bam.csv.enabled";
	
	/** CSV file's path. */
	@Documentation("CSV file's path.")
	String CSV_FILE_PATH = "bam.csv.file.path";

	/** CSV file's separator. */
	@Documentation("CSV file's separator.")
	String CSV_FILE_SEPARATOR = "bam.csv.file.separator";

}

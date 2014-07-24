package com.sirma.itt.emf.bam.configuration;

import com.sirma.itt.emf.configuration.Configuration;
import com.sirma.itt.emf.util.Documentation;

/**
 * Configuration properties for a BAM server.
 * 
 * @author Mihail Radkov
 */
@Documentation("Configuration for a BAM server.")
public interface BAMConfigurationProperties extends Configuration {

	/** Property defining if the agent will send to BAM. */
	@Documentation("Property defining if the agent will send to BAM.")
	String BAM_ENABLED = "bam.enabled";

	/** The BAM server's host. */
	@Documentation("BAM server's host")
	String BAM_HOST = "bam.address.host";

	/** The BAM server's port. */
	@Documentation("BAM server's port")
	String BAM_PORT = "bam.address.port";

	/** Address for BAM's streams receiver. */
	@Documentation("Address for BAM's streams receiver")
	String BAM_STREAMS_ADDRESS = "bam.address.stream.receiver";

	/** Address for BAM's EMF stream receiver. */
	@Documentation("Address for BAM's EMF stream receiver")
	String BAM_EMF_STREAM_ADDRESS = "bam.address.stream.emf";

	/** The BAM server's user. */
	@Documentation("Username for BAM server")
	String BAM_USERNAME = "bam.credentials.username";

	/** The BAM server's password. */
	@Documentation("Password for BAM server")
	String BAM_PASSWORD = "bam.credentials.password";

	/** Value in milliseconds for a connection timeout. */
	@Documentation("Value in milliseconds for a connection timeout.")
	String BAM_TIMEOUT = "bam.timeout";

	/** Table's name in the database where events are stored. */
	@Documentation("Table's name in the database where events are stored.")
	String BAM_ACTIVITY_TABLE = "bam.activity.table";

}

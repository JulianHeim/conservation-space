package com.sirma.itt.emf.bam.agent;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.emf.bam.configuration.BAMConfigurationProperties;
import com.sirma.itt.emf.bam.configuration.CSVConfigurationProperties;
import com.sirma.itt.emf.bam.csv.CSVLogger;
import com.sirma.itt.emf.bam.rest.BAMRESTPublisher;
import com.sirma.itt.emf.bam.rest.RESTPublisher;
import com.sirma.itt.emf.configuration.Config;

/**
 * Class that handles EMF events by sending them to a BAM server. It converts events to a JSON
 * representation, sends them to the server via a REST publisher and then logs the whole process
 * into a CSV file. This is an application scoped EJB bean so it can handles events during the life
 * cycle of the application.
 *
 * @author Mihail Radkov
 */
// TODO: Add persisting for unsent events.
@ApplicationScoped
public class BAMAgentImpl implements BAMAgent {

	/** Configuration property for the BAM server's host. */
	@Inject
	@Config(name = BAMConfigurationProperties.BAM_ENABLED, defaultValue = "false")
	private Boolean bamEnabled;

	/** Configuration property for the BAM server's host. */
	@Inject
	@Config(name = BAMConfigurationProperties.BAM_HOST)
	private String bamHost;

	/** Configuration property for the BAM server's port. */
	@Inject
	@Config(name = BAMConfigurationProperties.BAM_PORT)
	private String bamPort;

	/** Configuration property for the BAM server's streams receiver address. */
	@Inject
	@Config(name = BAMConfigurationProperties.BAM_STREAMS_ADDRESS, defaultValue = "/datareceiver/1.0.0/streams/")
	private String bamStreamsReceiverAddress;

	/** Configuration property for the BAM server's EMF stream receiver address. */
	@Inject
	@Config(name = BAMConfigurationProperties.BAM_EMF_STREAM_ADDRESS)
	private String bamEmfStreamAddress;

	/** Configuration property for the BAM server's user name. */
	@Inject
	@Config(name = BAMConfigurationProperties.BAM_USERNAME)
	private String bamUser;

	/** Configuration property for the BAM server's password. */
	@Inject
	@Config(name = BAMConfigurationProperties.BAM_PASSWORD)
	private String bamPassword;

	/** Configuration property for a connection timeout. */
	@Inject
	@Config(name = BAMConfigurationProperties.BAM_TIMEOUT, defaultValue = "30000")
	private String bamTimeout;

	/** Configuration property for enabling logging events into a CSV file. */
	@Inject
	@Config(name = CSVConfigurationProperties.CSV_ENABLED, defaultValue = "false")
	private Boolean csvEnabled;

	/** Configuration property for the CSV file's path. */
	@Inject
	@Config(name = CSVConfigurationProperties.CSV_FILE_PATH)
	private String csvFilePath;

	/** Configuration property for the CSV file's separator. */
	@Inject
	@Config(name = CSVConfigurationProperties.CSV_FILE_SEPARATOR, defaultValue = ";")
	private String csvFileSeparator;

	/** Logger used for displaying messages related to this class. */
	// TODO: BAMAgentImpl or BAMAgent ?
	private final Logger logger = LoggerFactory.getLogger(BAMAgentImpl.class);

	/** Used for publishing JSON formatted events using REST to a BAM server. */
	private RESTPublisher publisher;

	/** CSV file logger used for logging handled events. */
	private CSVLogger csvLogger;

	/**
	 * Called after instantiating an object of this class. Initializes
	 * components but first checks the BAM parameters (host/port/user/pass) and
	 * the CSV's (file path/separator). If some of them is null or empty a new
	 * IllegalArgumentException is thrown.
	 */
	@PostConstruct
	public void postConstruct() {
		if (bamEnabled) {
			this.publisher = new BAMRESTPublisher(bamHost, Integer.valueOf(bamPort), bamUser,
					bamPassword, Integer.valueOf(bamTimeout));
			initializeStreamDefinition("/com/sirma/itt/emf/bam/definitions/EMF_EVENTS.def",
					publisher, bamStreamsReceiverAddress, bamEmfStreamAddress);
		}
		if (csvEnabled) {
			this.csvLogger = new CSVLogger(csvFilePath, csvFileSeparator.charAt(0));
		}
		logger.info("Agent initialized.");
		// TODO: Check for unsent events.
	}

	/**
	 * Reads and send a stream definition for EMF events to the BAM server via the REST publisher.
	 * Returns a boolean value indicating if the sending was successful.
	 *
	 * @param streamFilePath
	 *            - the stream definition file path
	 * @param publisher
	 *            the REST publisher
	 * @param streamsAddress
	 *            - the address where the sent stream definitions are registered
	 * @param emfStreamAddress
	 *            - the address where the EMF events are sent
	 * @return true if the server responded with 202 status code and false otherwise
	 */
	public boolean initializeStreamDefinition(String streamFilePath,
			RESTPublisher publisher, String streamsAddress,
			String emfStreamAddress) {
		boolean flag = false;
		String streamDefinition = readFileAsString(streamFilePath);
		// TODO: In a configuration file/interface?
		publisher.setURI(streamsAddress);
		if (publisher.postMethod(streamDefinition) == 202) {
			flag = true;
		}
		// TODO: In a configuration file/interface?
		publisher.setURI(emfStreamAddress);
		return flag;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addEvent(List<Object> event) {
		if (!bamEnabled && !csvEnabled) {
			return;
		}
		// TODO: Synchronized block ?
		handleEvent(event, publisher, csvLogger);
	}

	/**
	 * Handles an event from EMF. Converts the event's payload to a json and monitors how long did
	 * it took to be sent to a server via a publisher and what was the response code. Finally saves
	 * this information into a CSV file via a CSV logger.
	 *
	 * @param event
	 *            the event to be handled
	 * @param publisher
	 *            the provided publisher sending data to a server
	 * @param csvLogger
	 *            the logger saving information into a CSV file
	 */
	public void handleEvent(List<Object> event, RESTPublisher publisher,
			CSVLogger csvLogger) {
		// TODO: Null checks?
		if (event != null) {
			if (publisher != null) {
				String jsonMessage = constructJsonMessage(event);

				long startTime = System.currentTimeMillis();
				int statusCode = publisher.postMethod(jsonMessage);
				long sendTime = System.currentTimeMillis() - startTime;
				// TODO: Persist the event if the server is down.
				if (csvLogger != null) {
					csvLogger.writeMessage(constructCSVMessage(event, statusCode, sendTime));
				}
				// // TODO: What's the performance impact for logging every event?
				if (logger.isTraceEnabled()) {
					logger.trace("Event handled - > " + jsonMessage);
				}
			}
		}
	}

	/**
	 * Constructs a JSON message containing the event's payload. Creates an instance of BAMEvent to
	 * construct it.
	 *
	 * @param event
	 *            the list containing the event's payload
	 * @return a JSON representation of the event
	 */
	public String constructJsonMessage(List<Object> event) {
		// TODO: If the payload's size is smaller than the count of the stream's
		// columns, the
		// BAM server throws an exception. Fix it more elegantly.
		// TODO: Null check?
		while (event.size() < 13) {
			event.add(null);
		}
		return new BAMEvent(event.toArray()).toJson();
	}

	/**
	 * Constructs an array of information from an event's payload with additional info of what was
	 * the response code and how long did it take to be send.
	 *
	 * @param event
	 *            the event's payload
	 * @param responseCode
	 *            - the response code from sending the event
	 * @param sendTime
	 *            - how long it took to be send
	 * @return the constructed string array with the required information
	 */
	public String[] constructCSVMessage(List<Object> event, int responseCode,
			long sendTime) {
		// TODO: Null check?
		event.add(0, String.valueOf(sendTime));
		event.add(0, String.valueOf(responseCode));
		// TODO: Check the content before converting.
		// http://stackoverflow.com/questions/14957964/java-array-to-string
		return event.toArray(new String[event.size()]);
	}

	/**
	 * Called before destroying an instance of this class. Closes the publisher
	 * and the CSV file logger.
	 */
	@PreDestroy
	public void preDestroy() {
		try {
			publisher.closePublisher();
			csvLogger.closeCSVLogger();
		} catch (IOException e) {
			logger.error("IO exception occured while closing streams.", e);
		}
		logger.info("Destroyed.");
	}

	/**
	 * Reads a file and returns it as string. If the provided file path is incorrect an
	 * IllegalArgumentException is thrown.
	 *
	 * @param filePath
	 *            a string value representing a path that points to a file to be read
	 * @return file as a string.
	 */
	public String readFileAsString(String filePath) {
		try {
			return IOUtils.toString(Thread.currentThread()
					.getContextClassLoader().getResourceAsStream(filePath));
		} catch (IOException e) {
			logger.error("IO exception occured while reading the definition file.");
			throw new IllegalArgumentException(e);
		} catch (NullPointerException e) {
			logger.error("Incorrect file path!");
			throw new IllegalArgumentException(e);
		}
	}

}

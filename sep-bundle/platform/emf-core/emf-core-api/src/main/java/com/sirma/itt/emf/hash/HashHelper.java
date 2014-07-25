package com.sirma.itt.emf.hash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.domain.Pair;

/**
 * Helper class for performing hash computations over lists and sets
 * 
 * @author BBonev
 */
public class HashHelper {

	/** The Constant prime. */
	public static final int PRIME = 31;
	/** The Constant LOGGER. */
	protected static final Logger LOGGER = LoggerFactory.getLogger(HashHelper.class);
	/** The Constant trace. */
	protected static final boolean trace = LOGGER.isTraceEnabled();

	/**
	 * Compute hash.
	 * 
	 * @param src
	 *            the src
	 * @return the int
	 */
	public static int computeHash(List<String> src) {
		int result = 1;
		if ((src != null) && !src.isEmpty()) {
			// changed hash code builder to iterate over the fields at the same
			// manner no matter of the order of the fields
			ArrayList<String> list = new ArrayList<>(src);
			Collections.sort(list);
			for (String string : list) {
				result = computeHash(result, string, "list(" + string + ")");
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Compute hash.
	 * 
	 * @param src
	 *            the src
	 * @return the int
	 */
	public static int computeHash(Set<String> src) {
		int result = 1;
		if ((src != null) && !src.isEmpty()) {
			// changed hash code builder to iterate over the fields at the same
			// manner no matter of the order of the fields
			ArrayList<String> list = new ArrayList<>(src);
			Collections.sort(list);
			for (String string : list) {
				result = computeHash(result, string, "list(" + string + ")");
			}
		} else {
			result = PRIME * result;
		}
		return result;
	}

	/**
	 * Computes hash for string values.
	 * 
	 * @param current
	 *            the current hash code
	 * @param value
	 *            the value
	 * @param name
	 *            the name of the field or some other custom string used for logging purposes
	 * @return the int
	 */
	public static int computeHash(int current, String value, String name) {
		int result = (PRIME * current) + (StringUtils.isNullOrEmpty(value) ? 0 : value.hashCode());
		log(name, "#" + name + "(" + value + ")=" + result);
		return result;
	}

	/**
	 * Computes hash for any other simple Objects.
	 * 
	 * @param current
	 *            the current hash code
	 * @param value
	 *            the value
	 * @param name
	 *            the name of the field or some other custom string used for logging purposes
	 * @return the int
	 */
	public static int computeHash(int current, Object value, String name) {
		if (value instanceof String) {
			return computeHash(current, (String) value, name);
		}
		int result = (PRIME * current) + (value == null ? 0 : value.hashCode());
		log(name, "#" + name + "(" + value + ")=" + result);
		return result;
	}

	/**
	 * Logs the given message.
	 * 
	 * @param string
	 *            the string
	 */
	protected static void log(String string) {
		if (trace) {
			LOGGER.trace(string);
		}
	}

	/**
	 * Logs the given message.
	 * 
	 * @param fieldName
	 *            the field name
	 * @param string
	 *            the string
	 */
	protected static void log(String fieldName, String string) {
		log(string);
		if (Boolean.TRUE.equals(HashStatistics.isStatisticsEnabled())) {
			HashStatistics.getStatistics(false).add(new Pair<>(fieldName, string));
		}
	}

}

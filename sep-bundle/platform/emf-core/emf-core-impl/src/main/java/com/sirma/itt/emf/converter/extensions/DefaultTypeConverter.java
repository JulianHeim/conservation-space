package com.sirma.itt.emf.converter.extensions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import com.sirma.itt.emf.converter.Converter;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.converter.TypeConverterProvider;
import com.sirma.itt.emf.exceptions.TypeConversionException;
import com.sirma.itt.emf.time.DateRange;
import com.sirma.itt.emf.time.ISO8601DateFormat;


/**
 * Support for generic conversion between types. Additional conversions may be added. Basic
 * inter-operability supported. Direct conversion and two stage conversions via Number are
 * supported.
 *
 * @author andyh
 * @author BBonev
 */
@ApplicationScoped
public class DefaultTypeConverter implements TypeConverterProvider {
	private static final Long LONG_FALSE = Long.valueOf(0L);
	private static final Long LONG_TRUE = Long.valueOf(1L);
	private static final Logger LOGGER = Logger.getLogger(DefaultTypeConverter.class);
	/**
	 * The Class ListToStringConverter.
	 *
	 * @param <L>
	 *            the list type type
	 */
	private static final class ListToStringConverter<L extends List> implements
			Converter<L, String> {

		/** The converter. */
		private TypeConverter converter;

		/**
		 * Instantiates a new list to string converter.
		 *
		 * @param converter
		 *            the converter
		 */
		public ListToStringConverter(TypeConverter converter) {
			this.converter = converter;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String convert(L source) {
			if (source != null) {
				StringBuilder result = new StringBuilder();
				int i = source.size();
				for (Object element : source) {
					result.append(converter.convert(String.class, element));
					i--;
					if (i > 0) {
						result.append(",");
					}
				}
				return result.toString();
			}
			return "";
		}
	}

	@Override
	public void register(TypeConverter converter) {
		//
		// From string
		//
		converter.addConverter(String.class, Class.class, new Converter<String, Class>() {
			@Override
			public Class convert(String source) {
				try {
					return Class.forName(source);
				} catch (ClassNotFoundException e) {
					throw new TypeConversionException("Failed to convert string to class: "
							+ source, e);
				}
			}
		});

		converter.addConverter(String.class, Boolean.class, new Converter<String, Boolean>() {
			@Override
			public Boolean convert(String source) {
				return Boolean.valueOf(source);
			}
		});

		converter.addConverter(String.class, Character.class, new Converter<String, Character>() {
			@Override
			public Character convert(String source) {
				if ((source == null) || (source.length() == 0)) {
					return null;
				}
				return Character.valueOf(source.charAt(0));
			}
		});

		converter.addConverter(String.class, Number.class, new Converter<String, Number>() {
			@Override
			public Number convert(String source) {
				try {
					return DecimalFormat.getNumberInstance().parse(source);
				} catch (ParseException e) {
					throw new TypeConversionException("Failed to parse number " + source, e);
				}
			}
		});

		converter.addConverter(String.class, Byte.class, new Converter<String, Byte>() {
			@Override
			public Byte convert(String source) {
				return Byte.valueOf(source);
			}
		});

		converter.addConverter(String.class, Short.class, new Converter<String, Short>() {
			@Override
			public Short convert(String source) {
				return Short.valueOf(source);
			}
		});

		converter.addConverter(String.class, Integer.class, new Converter<String, Integer>() {
			@Override
			public Integer convert(String source) {
				return Integer.valueOf(source);
			}
		});

		converter.addConverter(String.class, Long.class, new Converter<String, Long>() {
			@Override
			public Long convert(String source) {
				return Long.valueOf(source);
			}
		});

		converter.addConverter(String.class, Float.class, new Converter<String, Float>() {
			@Override
			public Float convert(String source) {
				return Float.valueOf(source);
			}
		});

		converter.addConverter(String.class, Double.class, new Converter<String, Double>() {
			@Override
			public Double convert(String source) {
				return Double.valueOf(source);
			}
		});

		converter.addConverter(String.class, BigInteger.class, new Converter<String, BigInteger>() {
			@Override
			public BigInteger convert(String source) {
				return new BigInteger(source);
			}
		});

		converter.addConverter(String.class, BigDecimal.class, new Converter<String, BigDecimal>() {
			@Override
			public BigDecimal convert(String source) {
				return new BigDecimal(source);
			}
		});

		converter.addConverter(String.class, Date.class, new Converter<String, Date>() {
			@Override
			public Date convert(String source) {
				try {
					Date date = ISO8601DateFormat.parse(source);
					return date;
				} catch (RuntimeException e) {
					throw new TypeConversionException("Failed to convert string " + source
							+ " to date", e);
				}
			}
		});

		converter.addConverter(String.class, InputStream.class,
				new Converter<String, InputStream>() {
					@Override
					public InputStream convert(String source) {
						try {
							return new ByteArrayInputStream(source.getBytes("UTF-8"));
						} catch (UnsupportedEncodingException e) {
							throw new TypeConversionException("Encoding not supported", e);
						}
					}
				});

		//
		// From Locale
		//

		converter.addConverter(Locale.class, String.class, new Converter<Locale, String>() {
			@Override
			public String convert(Locale source) {
				String localeStr = source.toString();
				if (localeStr.length() < 6) {
					localeStr += "_";
				}
				return localeStr;
			}
		});

		//
		// From enum
		//

		converter.addConverter(Enum.class, String.class, new Converter<Enum, String>() {
			@Override
			public String convert(Enum source) {
				return source.toString();
			}
		});

		// From Class

		converter.addConverter(Class.class, String.class, new Converter<Class, String>() {
			@Override
			public String convert(Class source) {
				return source.getName();
			}
		});

		//
		// Number to Subtypes and Date
		//

		converter.addConverter(Number.class, Boolean.class, new Converter<Number, Boolean>() {
			@Override
			public Boolean convert(Number source) {
				return new Boolean(source.longValue() > 0);
			}
		});

		converter.addConverter(Number.class, Byte.class, new Converter<Number, Byte>() {
			@Override
			public Byte convert(Number source) {
				return Byte.valueOf(source.byteValue());
			}
		});

		converter.addConverter(Number.class, Short.class, new Converter<Number, Short>() {
			@Override
			public Short convert(Number source) {
				return Short.valueOf(source.shortValue());
			}
		});

		converter.addConverter(Number.class, Integer.class, new Converter<Number, Integer>() {
			@Override
			public Integer convert(Number source) {
				return Integer.valueOf(source.intValue());
			}
		});

		converter.addConverter(Number.class, Long.class, new Converter<Number, Long>() {
			@Override
			public Long convert(Number source) {
				return Long.valueOf(source.longValue());
			}
		});

		converter.addConverter(Number.class, Float.class, new Converter<Number, Float>() {
			@Override
			public Float convert(Number source) {
				return Float.valueOf(source.floatValue());
			}
		});

		converter.addConverter(Number.class, Double.class, new Converter<Number, Double>() {
			@Override
			public Double convert(Number source) {
				return Double.valueOf(source.doubleValue());
			}
		});

		converter.addConverter(Number.class, Date.class, new Converter<Number, Date>() {
			@Override
			public Date convert(Number source) {
				return new Date(source.longValue());
			}
		});

		converter.addConverter(Number.class, String.class, new Converter<Number, String>() {
			@Override
			public String convert(Number source) {
				return source.toString();
			}
		});

		converter.addConverter(Number.class, BigInteger.class, new Converter<Number, BigInteger>() {
			@Override
			public BigInteger convert(Number source) {
				if (source instanceof BigDecimal) {
					return ((BigDecimal) source).toBigInteger();
				}
				return BigInteger.valueOf(source.longValue());
			}
		});

		converter.addConverter(Number.class, BigDecimal.class, new Converter<Number, BigDecimal>() {
			@Override
			public BigDecimal convert(Number source) {
				if (source instanceof BigInteger) {
					return new BigDecimal((BigInteger) source);
				}
				return BigDecimal.valueOf(source.longValue());
			}
		});

		converter.addDynamicTwoStageConverter(Number.class, String.class, InputStream.class);

		//
		// Date, Timestamp ->
		//

		converter.addConverter(Timestamp.class, Date.class, new Converter<Timestamp, Date>() {
			@Override
			public Date convert(Timestamp source) {
				return new Date(source.getTime());
			}
		});

		converter.addConverter(Date.class, Number.class, new Converter<Date, Number>() {
			@Override
			public Number convert(Date source) {
				return Long.valueOf(source.getTime());
			}
		});

		converter.addConverter(Date.class, String.class, new Converter<Date, String>() {
			@Override
			public String convert(Date source) {
				try {
					return ISO8601DateFormat.format(source);
				} catch (RuntimeException e) {
					throw new TypeConversionException("Failed to convert date " + source
							+ " to string", e);
				}
			}
		});

		converter.addConverter(Date.class, Calendar.class, new Converter<Date, Calendar>() {
			@Override
			public Calendar convert(Date source) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(source);
				return calendar;
			}
		});

		converter.addDynamicTwoStageConverter(Date.class, String.class, InputStream.class);

		//
		// Boolean ->
		//

		converter.addConverter(Boolean.class, Long.class, new Converter<Boolean, Long>() {
			@Override
			public Long convert(Boolean source) {
				return source.booleanValue() ? LONG_TRUE : LONG_FALSE;
			}
		});

		converter.addConverter(Boolean.class, String.class, new Converter<Boolean, String>() {
			@Override
			public String convert(Boolean source) {
				return source.toString();
			}
		});

		converter.addDynamicTwoStageConverter(Boolean.class, String.class, InputStream.class);

		//
		// Character ->
		//

		converter.addConverter(Character.class, String.class, new Converter<Character, String>() {
			@Override
			public String convert(Character source) {
				return source.toString();
			}
		});

		converter.addDynamicTwoStageConverter(Character.class, String.class, InputStream.class);

		//
		// Byte
		//

		converter.addConverter(Byte.class, String.class, new Converter<Byte, String>() {
			@Override
			public String convert(Byte source) {
				return source.toString();
			}
		});

		converter.addDynamicTwoStageConverter(Byte.class, String.class, InputStream.class);

		//
		// Short
		//

		converter.addConverter(Short.class, String.class, new Converter<Short, String>() {
			@Override
			public String convert(Short source) {
				return source.toString();
			}
		});

		converter.addDynamicTwoStageConverter(Short.class, String.class, InputStream.class);

		//
		// Integer
		//

		converter.addConverter(Integer.class, String.class, new Converter<Integer, String>() {
			@Override
			public String convert(Integer source) {
				return source.toString();
			}
		});

		converter.addDynamicTwoStageConverter(Integer.class, String.class, InputStream.class);

		//
		// Long
		//

		converter.addConverter(Long.class, String.class, new Converter<Long, String>() {
			@Override
			public String convert(Long source) {
				return source.toString();
			}
		});

		converter.addDynamicTwoStageConverter(Long.class, String.class, InputStream.class);

		//
		// Float
		//

		converter.addConverter(Float.class, String.class, new Converter<Float, String>() {
			@Override
			public String convert(Float source) {
				return source.toString();
			}
		});

		converter.addDynamicTwoStageConverter(Float.class, String.class, InputStream.class);

		//
		// Double
		//

		converter.addConverter(Double.class, String.class, new Converter<Double, String>() {
			@Override
			public String convert(Double source) {
				return source.toString();
			}
		});

		converter.addDynamicTwoStageConverter(Double.class, String.class, InputStream.class);

		//
		// BigInteger
		//

		converter.addConverter(BigInteger.class, String.class, new Converter<BigInteger, String>() {
			@Override
			public String convert(BigInteger source) {
				return source.toString();
			}
		});

		converter.addDynamicTwoStageConverter(BigInteger.class, String.class, InputStream.class);

		//
		// Calendar
		//

		converter.addConverter(Calendar.class, Date.class, new Converter<Calendar, Date>() {
			@Override
			public Date convert(Calendar source) {
				return source.getTime();
			}
		});

		converter.addConverter(Calendar.class, String.class, new Converter<Calendar, String>() {
			@Override
			public String convert(Calendar source) {
				try {
					return ISO8601DateFormat.format(source.getTime());
				} catch (RuntimeException e) {
					throw new TypeConversionException("Failed to convert date " + source
							+ " to string", e);
				}
			}
		});

		//
		// BigDecimal
		//

		converter.addConverter(BigDecimal.class, String.class, new Converter<BigDecimal, String>() {
			@Override
			public String convert(BigDecimal source) {
				return source.toString();
			}
		});
		converter.addDynamicTwoStageConverter(BigDecimal.class, String.class, InputStream.class);

		//
		// List converters
		//

		// list to to string conversions
		// we cannot register just to List interface but to concrete implementations and we should
		// register at least the 2 most used implementations
		converter.addConverter(ArrayList.class, String.class, new ListToStringConverter<ArrayList>(
				converter));
		converter.addConverter(LinkedList.class, String.class, new ListToStringConverter<LinkedList>(converter));
		converter.addConverter(AbstractList.class, String.class, new ListToStringConverter<AbstractList>(converter));
		converter.addConverter(String.class, List.class, new Converter<String, List>() {

			@Override
			public List convert(String source) {
				String[] split = source.split(",");
				List<String> list = new ArrayList<>(Arrays.asList(split));
				return list;
			}
		});
		converter.addConverter(JSONArray.class, List.class, new Converter<JSONArray, List>() {

			@Override
			public List convert(JSONArray source) {
				if (source == null) {
					return null;
				}
				try {
					List result = new ArrayList(source.length());
					for (int i = 0; i < source.length(); i++) {
						result.add(source.get(i));
					}
					return result;
				} catch (JSONException e) {
					LOGGER.warn("Failed to get the json array value due to", e);
				}
				return new ArrayList(1);
			}
		});

		//
		// Input Stream
		//

		converter.addConverter(InputStream.class, String.class,
				new Converter<InputStream, String>() {
					@Override
					public String convert(InputStream source) {
						try {
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							byte[] buffer = new byte[8192];
							int read;
							while ((read = source.read(buffer)) > 0) {
								out.write(buffer, 0, read);
							}
							byte[] data = out.toByteArray();
							return new String(data, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							throw new TypeConversionException(
									"Cannot convert input stream to String.", e);
						} catch (IOException e) {
							throw new TypeConversionException(
									"Conversion from stream to string failed", e);
						} finally {
							if (source != null) {
								try {
									source.close();
								} catch (IOException e) {
								}
							}
						}
					}
				});

		converter.addDynamicTwoStageConverter(InputStream.class, String.class, Date.class);

		converter.addDynamicTwoStageConverter(InputStream.class, String.class, Double.class);

		converter.addDynamicTwoStageConverter(InputStream.class, String.class, Long.class);

		converter.addDynamicTwoStageConverter(InputStream.class, String.class, Boolean.class);

		converter.addConverter(DateRange.class, String.class, new Converter<DateRange, String>() {
			private static final String DATE_FROM_SUFFIX = "T00:00:00";
			/** The Constant DATE_SUFFIX. */
			private static final String DATE_TO_SUFFIX = "T23:59:59";
			/** example 2012-09-26T00:00:00. */
			public final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

			@Override
			public String convert(DateRange value) {
				return createDateRange(value.getFirst(), value.getSecond());
			}

			/**
			 * Creates the range.
			 *
			 * @param from
			 *            the start date
			 * @param to
			 *            the end date
			 * @return the string
			 */
			private String createDateRange(Serializable from, Serializable to) {
				String result = null;
				if ((from != null) && !from.toString().isEmpty()) {
					String fromData = DATE_FORMAT.format(from);
					if ((to != null) && !to.toString().isEmpty()) {
						result = "[\"" + fromData + DATE_FROM_SUFFIX + "\" TO \""
								+ DATE_FORMAT.format(to) + DATE_TO_SUFFIX + "\"]";
					} else {
						result = "[\"" + fromData + DATE_FROM_SUFFIX + "\" TO MAX]";
					}
				} else if ((to != null) && !to.toString().isEmpty()) {
					String format = DATE_FORMAT.format(to) + DATE_TO_SUFFIX;
					result = "[MIN TO \"" + format + "\"]";
				}
				return result;

			}
		});
	}

}

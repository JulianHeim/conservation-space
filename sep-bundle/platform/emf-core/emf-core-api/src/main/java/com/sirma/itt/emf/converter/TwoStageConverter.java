package com.sirma.itt.emf.converter;

/**
 * Support for chaining conversions.
 *
 * @param <F>
 *            From Type
 * @param <I>
 *            Intermediate type
 * @param <T>
 *            To Type
 * @author andyh
 */
public class TwoStageConverter<F, I, T> implements Converter<F, T> {

	/** The first. */
	@SuppressWarnings("rawtypes")
	private Converter first;

	/** The second. */
	@SuppressWarnings("rawtypes")
	private Converter second;

	/**
	 * Instantiates a new two stage converter.
	 *
	 * @param first
	 *            the first
	 * @param second
	 *            the second
	 */
	@SuppressWarnings("rawtypes")
	public TwoStageConverter(Converter first, Converter second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T convert(F source) {
		return (T) second.convert(first.convert(source));
	}
}
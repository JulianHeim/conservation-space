package com.sirma.itt.emf.web.converter;

import java.text.MessageFormat;
import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.DateTimeConverter;
import javax.inject.Inject;
import javax.inject.Named;

import com.sirma.itt.emf.configuration.Config;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.web.config.EmfWebConfigurationProperties;

/**
 * Faces converter - converts inputed string to java.sql.Date. <br />
 * <b>NOTE: Because its not possible to inject inside FacesConverter we should use a workaround. We
 * make the convertor a managed bean and it is binded trough EL expression instead trough setter.
 * https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-763 http://stackoverflow.com/questions
 * /13156671/how-can-i-inject-in-facesconverter/13156834#13156834</b>
 *
 * @author svelikov
 */
@Named
@ApplicationScoped
public class EmfDateConverter extends DateTimeConverter {

	/** The converter date format pattern. */
	@Inject
	@Config(name = EmfWebConfigurationProperties.CONVERTER_DATE_FORMAT, defaultValue = "dd.MM.yyyy")
	private String converterDateFormatPattern;

	/** The label provider. */
	@Inject
	private LabelProvider labelProvider;

	/**
	 * Converts a string to java.sql.Date.
	 *
	 * @param context
	 *            FacesContext.
	 * @param component
	 *            Faces component.
	 * @param value
	 *            String value.
	 * @return the date as java.sql.Date
	 */
	@Override
	public Object getAsObject(final FacesContext context, final UIComponent component,
			final String value) {
		Date date = null;
		try {
			this.setPattern(converterDateFormatPattern);
			setTimeZone(null);
			date = (Date) super.getAsObject(context, component, value);
		} catch (final Exception e) {
			final String errorMessage = MessageFormat.format(
					labelProvider.getValue("date.converter.error"), converterDateFormatPattern);
			throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
					errorMessage, errorMessage));
		}
		return date;
	}

	/**
	 * Converts an object to String.
	 *
	 * @param context
	 *            FacesContext.
	 * @param component
	 *            Faces component.
	 * @param obj
	 *            Object that will be converted.
	 * @return the formated date
	 */
	@Override
	public String getAsString(final FacesContext context, final UIComponent component,
			final Object obj) {
		String formattedDate = null;
		this.setPattern(converterDateFormatPattern);
		setTimeZone(null);
		formattedDate = super.getAsString(context, component, obj);
		return formattedDate;
	}

}
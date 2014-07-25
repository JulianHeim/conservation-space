//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB)
// Reference Implementation, v2.2.6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source
// schema.
// Generated on: 2013.02.15 at 03:17:22 PM EET
//

package com.sirma.itt.cmf.beans.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.sirma.itt.emf.definition.model.jaxb.FilterDefinitions;
import com.sirma.itt.emf.definition.model.jaxb.Labels;

/**
 * <p>
 * Java class for anonymous complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="documentDefinition" type="{}documentDefinition" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}filterDefinitions" minOccurs="0"/>
 *         &lt;element ref="{}labels" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "documentDefinition", "filterDefinitions", "labels" })
@XmlRootElement(name = "documentDefinitions")
public class DocumentDefinitions {

	/** The document definition. */
	protected List<DocumentDefinition> documentDefinition;

	/** The filter definitions. */
	protected FilterDefinitions filterDefinitions;

	/** The labels. */
	protected Labels labels;

	/**
	 * Gets the value of the documentDefinition property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a
	 * snapshot. Therefore any modification you make to the returned list will
	 * be present inside the JAXB object. This is why there is not a
	 * <CODE>set</CODE> method for the documentDefinition property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getDocumentDefinition().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 *
	 * @return the document definition {@link DocumentDefinition }
	 */
	public List<DocumentDefinition> getDocumentDefinition() {
		if (documentDefinition == null) {
			documentDefinition = new ArrayList<DocumentDefinition>();
		}
		return documentDefinition;
	}

	/**
	 * Gets the value of the filterDefinitions property.
	 *
	 * @return the filter definitions possible object is
	 *         {@link FilterDefinitions }
	 */
	public FilterDefinitions getFilterDefinitions() {
		return filterDefinitions;
	}

	/**
	 * Sets the value of the filterDefinitions property.
	 *
	 * @param value
	 *            allowed object is {@link FilterDefinitions }
	 */
	public void setFilterDefinitions(FilterDefinitions value) {
		filterDefinitions = value;
	}

	/**
	 * Gets the value of the labels property.
	 *
	 * @return the labels possible object is {@link Labels }
	 */
	public Labels getLabels() {
		return labels;
	}

	/**
	 * Sets the value of the labels property.
	 *
	 * @param value
	 *            allowed object is {@link Labels }
	 */
	public void setLabels(Labels value) {
		labels = value;
	}

}
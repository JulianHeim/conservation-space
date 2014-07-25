//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB)
// Reference Implementation, v2.2.5-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source
// schema.
// Generated on: 2012.12.14 at 09:54:59 AM EET
//

package com.sirma.itt.emf.definition.model.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for controlDefinition complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="controlDefinition">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="control-param" type="{}controlParam" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="ui-param" type="{}uiParam" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="fields" type="{}complexFieldsDefinition" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "controlDefinition", propOrder = { "controlParam", "uiParam", "fields" })
public class ControlDefinition {

	/** The control param. */
	@XmlElement(name = "control-param")
	protected List<ControlParam> controlParam;

	/** The ui param. */
	@XmlElement(name = "ui-param")
	protected List<UiParam> uiParam;

	/** The fields. */
	protected ComplexFieldsDefinition fields;

	/** The id. */
	@XmlAttribute(name = "id", required = true)
	protected String id;

	/**
	 * Gets the value of the controlParam property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a
	 * snapshot. Therefore any modification you make to the returned list will
	 * be present inside the JAXB object. This is why there is not a
	 * <CODE>set</CODE> method for the controlParam property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getControlParam().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 *
	 * @return the control param {@link ControlParam }
	 */
	public List<ControlParam> getControlParam() {
		if (controlParam == null) {
			controlParam = new ArrayList<ControlParam>();
		}
		return controlParam;
	}

	/**
	 * Gets the value of the uiParam property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a
	 * snapshot. Therefore any modification you make to the returned list will
	 * be present inside the JAXB object. This is why there is not a
	 * <CODE>set</CODE> method for the uiParam property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getUiParam().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link UiParam }
	 *
	 * @return the ui param
	 */
	public List<UiParam> getUiParam() {
		if (uiParam == null) {
			uiParam = new ArrayList<UiParam>();
		}
		return uiParam;
	}

	/**
	 * Gets the value of the fields property.
	 *
	 * @return possible object is {@link ComplexFieldsDefinition }
	 */
	public ComplexFieldsDefinition getFields() {
		return fields;
	}

	/**
	 * Sets the value of the fields property.
	 *
	 * @param value
	 *            allowed object is {@link ComplexFieldsDefinition }
	 */
	public void setFields(ComplexFieldsDefinition value) {
		fields = value;
	}

	/**
	 * Gets the value of the id property.
	 *
	 * @return possible object is {@link String }
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the value of the id property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setId(String value) {
		id = value;
	}

}
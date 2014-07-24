
package com.sirma.codelist.ws.stub;

import javax.xml.ws.WebFault;


// TODO: Auto-generated Javadoc
/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.3-b02-
 * Generated source version: 2.0
 * 
 */
@WebFault(name = "DataServiceFault", targetNamespace = "http://ws.wso2.org/dataservice")
public class DataServiceFault
    extends Exception
{

    /**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = 4062272645366195034L;
	/**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private String faultInfo;

    /**
     * Instantiates a new data service fault.
     *
     * @param message the message
     * @param faultInfo the fault info
     */
    public DataServiceFault(String message, String faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * Instantiates a new data service fault.
     *
     * @param message the message
     * @param faultInfo the fault info
     * @param cause the cause
     */
    public DataServiceFault(String message, String faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * Gets the fault info.
     *
     * @return the fault info
     * returns fault bean: java.lang.String
     */
    public String getFaultInfo() {
        return faultInfo;
    }

}
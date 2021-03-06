/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.query.QueryProperty;


// TODO: Auto-generated Javadoc
/**
 * Contains the possible properties which can be used in a {@link HistoricProcessInstanceQueryProperty}.
 * 
 * @author Joram Barrez
 */
public class HistoricProcessInstanceQueryProperty implements QueryProperty {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** The Constant properties. */
  private static final Map<String, HistoricProcessInstanceQueryProperty> properties = new HashMap<String, HistoricProcessInstanceQueryProperty>();

  /** The Constant PROCESS_INSTANCE_ID_. */
  public static final HistoricProcessInstanceQueryProperty PROCESS_INSTANCE_ID_ = new HistoricProcessInstanceQueryProperty("PROC_INST_ID_");
  
  /** The Constant PROCESS_DEFINITION_ID. */
  public static final HistoricProcessInstanceQueryProperty PROCESS_DEFINITION_ID = new HistoricProcessInstanceQueryProperty("PROC_DEF_ID_");
  
  /** The Constant BUSINESS_KEY. */
  public static final HistoricProcessInstanceQueryProperty BUSINESS_KEY = new HistoricProcessInstanceQueryProperty("BUSINESS_KEY_");
  
  /** The Constant START_TIME. */
  public static final HistoricProcessInstanceQueryProperty START_TIME = new HistoricProcessInstanceQueryProperty("START_TIME_");
  
  /** The Constant END_TIME. */
  public static final HistoricProcessInstanceQueryProperty END_TIME = new HistoricProcessInstanceQueryProperty("END_TIME_");
  
  /** The Constant DURATION. */
  public static final HistoricProcessInstanceQueryProperty DURATION = new HistoricProcessInstanceQueryProperty("DURATION_");
  
  /** The name. */
  private String name;

  /**
   * Instantiates a new historic process instance query property.
   *
   * @param name the name
   */
  public HistoricProcessInstanceQueryProperty(String name) {
    this.name = name;
    properties.put(name, this);
  }

  /* (non-Javadoc)
   * @see org.activiti.engine.query.QueryProperty#getName()
   */
  public String getName() {
    return name;
  }
  
  /**
   * Find by name.
   *
   * @param propertyName the property name
   * @return the historic process instance query property
   */
  public static HistoricProcessInstanceQueryProperty findByName(String propertyName) {
    return properties.get(propertyName);
  }

}

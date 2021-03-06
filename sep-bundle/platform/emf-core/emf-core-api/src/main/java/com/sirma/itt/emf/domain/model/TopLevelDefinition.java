package com.sirma.itt.emf.domain.model;

import com.sirma.itt.emf.instance.model.TenantAware;

/**
 * Defines common methods for top level definition model
 *
 * @author BBonev
 */
public interface TopLevelDefinition extends DefinitionModel, TenantAware {

	/**
	 * Gets the parent definition id.
	 *
	 * @return the parent definition id
	 */
	String getParentDefinitionId();

	/**
	 * Gets the dms id.
	 *
	 * @return the dms id
	 */
	String getDmsId();

	/**
	 * Sets the dms id.
	 *
	 * @param dmsId
	 *            the new dms id
	 */
	void setDmsId(String dmsId);

	/**
	 * Gets the container.
	 *
	 * @return the container
	 */
	@Override
	String getContainer();

	/**
	 * Sets the container.
	 *
	 * @param container
	 *            the new container
	 */
	@Override
	void setContainer(String container);

	/**
	 * Checks if the given definition is abstract.
	 *
	 * @return true, if is abstract
	 */
	boolean isAbstract();

	/**
	 * Gets the revision.
	 * 
	 * @return the revision
	 */
	@Override
	Long getRevision();

	/**
	 * Sets the revision.
	 * 
	 * @param revision
	 *            the new revision
	 */
	void setRevision(Long revision);

}

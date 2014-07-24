/**
 * 
 */
package com.sirma.itt.emf.bam.integration;

import com.sirma.itt.emf.domain.model.Entity;

/**
 * Mockup class implementing the <i>Entity</i> interface that is used for testing.
 * 
 * @author Mihail Radkov
 */
public class MockupEntity implements Entity<Long> {

	/** Identifier for the entity. */
	private long id;

	/**
	 * Class constructor.
	 */
	public MockupEntity() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long getId() {
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

}

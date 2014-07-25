package com.sirma.itt.emf.db;

import java.io.Serializable;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.sirma.itt.emf.domain.Pair;
import com.sirma.itt.emf.domain.model.Entity;
import com.sirma.itt.emf.plugin.Chaining;

/**
 * @author BBonev
 */
@Chaining
@Stateless
public class ChainingDbDao extends AbstractDbDao {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = 1298352452506262418L;

	@Inject
	private DbDao relationalDbDao;

	@Inject
	@SemanticDb
	private javax.enterprise.inject.Instance<DbDao> semanticDbDao;
	/**
	 * {@inheritDoc}
	 */
	@Override
	public <S extends Serializable, E extends Entity<S>> E saveOrUpdate(E entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends Serializable, E extends Entity<S>> E saveOrUpdate(E entity, E oldEntity) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <S extends Serializable, E extends Entity<S>> E find(Class<E> clazz, Object id) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <S extends Serializable, E extends Entity<S>> E refresh(E entity) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <S extends Serializable, E extends Entity<S>> void delete(Class<E> clazz,
			Serializable entityId) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <R, E extends Pair<String, Object>> List<R> fetchWithNamed(String namedQuery,
			List<E> params) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <R, E extends Pair<String, Object>> List<R> fetch(String query, List<E> params) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <E extends Pair<String, Object>> int executeUpdate(String namedQuery, List<E> params) {
		// TODO Auto-generated method stub
		return 0;
	}

}

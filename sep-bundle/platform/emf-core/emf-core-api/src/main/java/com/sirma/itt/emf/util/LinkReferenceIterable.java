package com.sirma.itt.emf.util;

import java.util.Collection;
import java.util.Iterator;

import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.link.LinkReference;

/**
 * Implementation for {@link Iterable} interface to provide automatic iteration of a collection of
 * {@link LinkReference}s. When creating new instance provide the source collection of
 * {@link LinkReference}.
 *
 * @author BBonev
 */
public class LinkReferenceIterable implements Iterable<InstanceReference>,
		Collection<InstanceReference> {

	/** The source. */
	private Collection<LinkReference> source;

	/** The is from. */
	private boolean isFrom;

	/**
	 * Instantiates a new link reference iterator that will return {@link LinkReference#getTo()}
	 * instances.
	 *
	 * @param source
	 *            the source collection to iterate
	 */
	public LinkReferenceIterable(Collection<LinkReference> source) {
		this(source, false);
	}

	/**
	 * Instantiates a new link reference iterator that could iterate over
	 * {@link LinkReference#getTo()} or {@link LinkReference#getFrom()} references.
	 *
	 * @param source
	 *            the source collection to iterate
	 * @param isFrom
	 *            if <code>true</code> the iterator will return only {@link LinkReference#getFrom()}
	 *            references and if <code>false</code> only {@link LinkReference#getTo()}
	 */
	public LinkReferenceIterable(Collection<LinkReference> source, boolean isFrom) {
		this.source = source;
		this.isFrom = isFrom;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<InstanceReference> iterator() {
		return new LinkReferenceIterator(source.iterator(), isFrom);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LinkReferenceIterable [isFrom=");
		builder.append(isFrom);
		builder.append(", source=");
		builder.append(source);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int size() {
		return source.size();
	}

	@Override
	public boolean isEmpty() {
		return source.isEmpty();
	}

	@Override
	public boolean contains(Object paramObject) {
		return source.contains(paramObject);
	}

	@Override
	public Object[] toArray() {
		return new Object[0];
	}

	@Override
	public <T> T[] toArray(T[] paramArrayOfT) {
		return paramArrayOfT;
	}

	@Override
	public boolean add(InstanceReference paramE) {
		return false;
	}

	@Override
	public boolean remove(Object paramObject) {
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> paramCollection) {
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends InstanceReference> paramCollection) {
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> paramCollection) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> paramCollection) {
		return false;
	}

	@Override
	public void clear() {
		source.clear();
	}

	/**
	 * Iterator proxy implementation to iterate a iterator of {@link LinkReference}s
	 *
	 * @author BBonev
	 */
	private static class LinkReferenceIterator implements Iterator<InstanceReference> {

		/** The iterator. */
		private final Iterator<LinkReference> iterator;

		/** The from. */
		private final boolean from;

		/**
		 * Instantiates a new link reference iterator.
		 *
		 * @param iterator
		 *            the iterator
		 * @param isFrom
		 *            the is from
		 */
		public LinkReferenceIterator(Iterator<LinkReference> iterator, boolean isFrom) {
			this.iterator = iterator;
			from = isFrom;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public InstanceReference next() {
			LinkReference next = iterator.next();
			return from ? next.getFrom() : next.getTo();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			iterator.remove();
		}
	}
}

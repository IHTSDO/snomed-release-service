package org.ihtsdo.buildcloud.service.helper;

/**
 * Closure to initialize lazy hibernate relationships within the transaction scope.
 * @param <T>
 */
public abstract class LazyInitializer<T> {

	public abstract void initializeLazyRelationships(T entity);

}

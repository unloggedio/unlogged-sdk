package io.unlogged.test;

import java.util.List;
import java.util.Optional;

public interface JpaRepository<T, ID> {

    /**
     * Flushes all pending changes to the database.
     */
    void flush();

    /**
     * Saves an entity and flushes changes instantly.
     *
     * @param entity entity to be saved. Must not be {@literal null}.
     * @return the saved entity
     */
    <S extends T> S saveAndFlush(S entity);

    /**
     * Saves all entities and flushes changes instantly.
     *
     * @param entities entities to be saved. Must not be {@literal null}.
     * @return the saved entities
     * @since 2.5
     */
    <S extends T> List<S> saveAllAndFlush(Iterable<S> entities);

    /**
     * Deletes the given entities in a batch which means it will create a single query. This kind of operation leaves JPAs
     * first level cache and the database out of sync. Consider flushing the {@link EntityManager} before calling this
     * method.
     *
     * @param entities entities to be deleted. Must not be {@literal null}.
     * @deprecated Use {@link #deleteAllInBatch(Iterable)} instead.
     */
    @Deprecated
    default void deleteInBatch(Iterable<T> entities) {
        deleteAllInBatch(entities);
    }

    /**
     * Deletes the given entities in a batch which means it will create a single query. This kind of operation leaves JPAs
     * first level cache and the database out of sync. Consider flushing the {@link EntityManager} before calling this
     * method.
     *
     * @param entities entities to be deleted. Must not be {@literal null}.
     * @since 2.5
     */
    void deleteAllInBatch(Iterable<T> entities);

    /**
     * Deletes the entities identified by the given ids using a single query. This kind of operation leaves JPAs first
     * level cache and the database out of sync. Consider flushing the {@link EntityManager} before calling this method.
     *
     * @param ids the ids of the entities to be deleted. Must not be {@literal null}.
     * @since 2.5
     */
    void deleteAllByIdInBatch(Iterable<ID> ids);

    /**
     * Deletes all entities in a batch call.
     */
    void deleteAllInBatch();

    /**
     * Returns a reference to the entity with the given identifier. Depending on how the JPA persistence provider is
     * implemented this is very likely to always return an instance and throw an
     * {@link jakarta.persistence.EntityNotFoundException} on first access. Some of them will reject invalid identifiers
     * immediately.
     *
     * @param id must not be {@literal null}.
     * @return a reference to the entity with the given identifier.
     * @see EntityManager#getReference(Class, Object) for details on when an exception is thrown.
     * @deprecated use {@link JpaRepository#getReferenceById(ID)} instead.
     */
    @Deprecated
    T getOne(ID id);

    /**
     * Returns a reference to the entity with the given identifier. Depending on how the JPA persistence provider is
     * implemented this is very likely to always return an instance and throw an
     * {@link jakarta.persistence.EntityNotFoundException} on first access. Some of them will reject invalid identifiers
     * immediately.
     *
     * @param id must not be {@literal null}.
     * @return a reference to the entity with the given identifier.
     * @see EntityManager#getReference(Class, Object) for details on when an exception is thrown.
     * @since 2.5
     * @deprecated use {@link JpaRepository#getReferenceById(ID)} instead.
     */
    @Deprecated
    T getById(ID id);

    /**
     * Returns a reference to the entity with the given identifier. Depending on how the JPA persistence provider is
     * implemented this is very likely to always return an instance and throw an
     * {@link jakarta.persistence.EntityNotFoundException} on first access. Some of them will reject invalid identifiers
     * immediately.
     *
     * @param id must not be {@literal null}.
     * @return a reference to the entity with the given identifier.
     * @see EntityManager#getReference(Class, Object) for details on when an exception is thrown.
     * @since 2.7
     */
    Optional<Authority> getReferenceById(ID id);

}
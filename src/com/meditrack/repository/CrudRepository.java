package com.meditrack.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface defining standard CRUD operations.
 *
 * Demonstrates:
 * - ABSTRACTION: Decouples business logic from physical persistence mechanisms.
 * - GENERICS: Reusable typed persistence contract for any entity type {@code T}
 *   and primary key {@code ID}.
 *
 * @param <T>  the entity type
 * @param <ID> the primary key identifier type
 */
public interface CrudRepository<T, ID> {

    /**
     * Persists a new entity.
     *
     * @param entity the entity to save
     */
    void save(T entity);

    /**
     * Retrieves an entity by its unique identifier.
     *
     * @param id primary key identifier
     * @return Optional containing the entity if found, or empty Optional
     */
    Optional<T> findById(ID id);

    /**
     * Retrieves all persisted entities.
     *
     * @return list of all entities
     */
    List<T> findAll();

    /**
     * Updates an existing entity.
     *
     * @param entity the entity with modified fields
     */
    void update(T entity);

    /**
     * Deletes an entity matching the given identifier.
     *
     * @param id identifier of entity to delete
     * @return true if an entity was deleted, false otherwise
     */
    boolean deleteById(ID id);
}

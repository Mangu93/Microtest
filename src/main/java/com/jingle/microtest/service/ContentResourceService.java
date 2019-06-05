package com.jingle.microtest.service;

import com.jingle.microtest.domain.Contents;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link Contents}.
 */
public interface ContentResourceService {

    /**
     * Save a content.
     *
     * @param contents the entity to save.
     * @return the persisted entity.
     */
    Contents save(Contents contents);

    /**
     * Get all the contents.
     *
     * @return the list of entities.
     */
    List<Contents> findAll();


    /**
     * Get the "id" content.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<Contents> findOne(Long id);

    /**
     * Delete the "id" content.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}

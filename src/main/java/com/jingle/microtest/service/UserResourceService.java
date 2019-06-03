package com.jingle.microtest.service;

import com.jingle.microtest.domain.UserResource;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link UserResource}.
 */
public interface UserResourceService {

    /**
     * Save a userResource.
     *
     * @param userResource the entity to save.
     * @return the persisted entity.
     */
    UserResource save(UserResource userResource);

    /**
     * Get all the userResources.
     *
     * @return the list of entities.
     */
    List<UserResource> findAll();


    /**
     * Get the "id" userResource.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<UserResource> findOne(Long id);

    /**
     * Delete the "id" userResource.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}

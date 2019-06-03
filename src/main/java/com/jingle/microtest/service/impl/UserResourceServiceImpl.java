package com.jingle.microtest.service.impl;

import com.jingle.microtest.service.UserResourceService;
import com.jingle.microtest.domain.UserResource;
import com.jingle.microtest.repository.UserResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for managing {@link UserResource}.
 */
@Service
@Transactional
public class UserResourceServiceImpl implements UserResourceService {

    private final Logger log = LoggerFactory.getLogger(UserResourceServiceImpl.class);

    private final UserResourceRepository userResourceRepository;

    public UserResourceServiceImpl(UserResourceRepository userResourceRepository) {
        this.userResourceRepository = userResourceRepository;
    }

    /**
     * Save a userResource.
     *
     * @param userResource the entity to save.
     * @return the persisted entity.
     */
    @Override
    public UserResource save(UserResource userResource) {
        log.debug("Request to save UserResource : {}", userResource);
        return userResourceRepository.save(userResource);
    }

    /**
     * Get all the userResources.
     *
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserResource> findAll() {
        log.debug("Request to get all UserResources");
        return userResourceRepository.findAll();
    }


    /**
     * Get one userResource by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<UserResource> findOne(Long id) {
        log.debug("Request to get UserResource : {}", id);
        return userResourceRepository.findById(id);
    }

    /**
     * Delete the userResource by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete UserResource : {}", id);
        userResourceRepository.deleteById(id);
    }
}

package com.jingle.microtest.service.impl;

import com.jingle.microtest.domain.Contents;
import com.jingle.microtest.service.ContentResourceService;
import com.jingle.microtest.repository.ContentResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for managing {@link Contents}.
 */
@Service
@Transactional
public class ContentResourceServiceImpl implements ContentResourceService {

    private final Logger log = LoggerFactory.getLogger(ContentResourceServiceImpl.class);

    private final ContentResourceRepository contentResourceRepository;

    public ContentResourceServiceImpl(ContentResourceRepository contentResourceRepository) {
        this.contentResourceRepository = contentResourceRepository;
    }

    /**
     * Save a contents.
     *
     * @param contents the entity to save.
     * @return the persisted entity.
     */
    @Override
    public Contents save(Contents contents) {
        log.debug("Request to save Contents : {}", contents);
        return contentResourceRepository.save(contents);
    }

    /**
     * Get all the contents.
     *
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Contents> findAll() {
        log.debug("Request to get all contents");
        return contentResourceRepository.findAll();
    }


    /**
     * Get one content by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Contents> findOne(Long id) {
        log.debug("Request to get Contents : {}", id);
        return contentResourceRepository.findById(id);
    }

    /**
     * Delete the content by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete Contents : {}", id);
        contentResourceRepository.deleteById(id);
    }
}

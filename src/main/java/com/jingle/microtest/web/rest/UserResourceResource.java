package com.jingle.microtest.web.rest;

import com.jingle.microtest.domain.UserResource;
import com.jingle.microtest.service.UserResourceService;
import com.jingle.microtest.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link com.jingle.microtest.domain.UserResource}.
 */
@RestController
@RequestMapping("/api")
public class UserResourceResource {

    private final Logger log = LoggerFactory.getLogger(UserResourceResource.class);

    private static final String ENTITY_NAME = "userResource";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserResourceService userResourceService;

    public UserResourceResource(UserResourceService userResourceService) {
        this.userResourceService = userResourceService;
    }

    /**
     * {@code POST  /user-resources} : Create a new userResource.
     *
     * @param userResource the userResource to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new userResource, or with status {@code 400 (Bad Request)} if the userResource has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/user-resources")
    public ResponseEntity<UserResource> createUserResource(@Valid @RequestBody UserResource userResource) throws URISyntaxException {
        log.debug("REST request to save UserResource : {}", userResource);
        if (userResource.getId() != null) {
            throw new BadRequestAlertException("A new userResource cannot already have an ID", ENTITY_NAME, "idexists");
        }
        UserResource result = userResourceService.save(userResource);
        return ResponseEntity.created(new URI("/api/user-resources/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /user-resources} : Updates an existing userResource.
     *
     * @param userResource the userResource to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userResource,
     * or with status {@code 400 (Bad Request)} if the userResource is not valid,
     * or with status {@code 500 (Internal Server Error)} if the userResource couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/user-resources")
    public ResponseEntity<UserResource> updateUserResource(@Valid @RequestBody UserResource userResource) throws URISyntaxException {
        log.debug("REST request to update UserResource : {}", userResource);
        if (userResource.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        UserResource result = userResourceService.save(userResource);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, userResource.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /user-resources} : get all the userResources.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of userResources in body.
     */
    @GetMapping("/user-resources")
    public List<UserResource> getAllUserResources() {
        log.debug("REST request to get all UserResources");

        return userResourceService.findAll();
    }

    /**
     * {@code GET  /user-resources/:id} : get the "id" userResource.
     *
     * @param id the id of the userResource to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the userResource, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/user-resources/{id}")
    public ResponseEntity<UserResource> getUserResource(@PathVariable Long id) {
        log.debug("REST request to get UserResource : {}", id);
        Optional<UserResource> userResource = userResourceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(userResource);
    }

    /**
     * {@code DELETE  /user-resources/:id} : delete the "id" userResource.
     *
     * @param id the id of the userResource to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/user-resources/{id}")
    public ResponseEntity<Void> deleteUserResource(@PathVariable Long id) {
        log.debug("REST request to delete UserResource : {}", id);
        userResourceService.delete(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString())).build();
    }
}

package com.jingle.microtest.web.rest;

import com.jingle.microtest.domain.User;
import com.jingle.microtest.domain.UserResource;
import com.jingle.microtest.repository.UserRepository;
import com.jingle.microtest.service.UserResourceService;
import com.jingle.microtest.web.rest.errors.BadRequestAlertException;
import io.github.jhipster.web.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private final UserRepository userRepository;

    public UserResourceResource(UserResourceService userResourceService, UserRepository userRepository) {
        this.userResourceService = userResourceService;
        this.userRepository = userRepository;
    }

    /**
     * {@code POST  /user-resources} : Create a new userResource.
     *
     * @param userResource the userResource to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new userResource, or with status {@code 400 (Bad Request)} if the userResource has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/user-resources")
    public ResponseEntity<UserResource> createUserResource(@Valid @RequestBody UserResource userResource, HttpServletRequest request) throws URISyntaxException {
        log.debug("REST request to save UserResource : {}", userResource);
        if (userResource.getId() != null) {
            throw new BadRequestAlertException("A new userResource cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (userResource.getUserBelongsTo() == null) {
            Optional<User> optionalUser = userRepository.findOneByLogin(request.getRemoteUser());
            optionalUser.ifPresent(userResource::setUserBelongsTo);
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
    public ResponseEntity<UserResource> updateUserResource(@Valid @RequestBody UserResource userResource, HttpServletRequest request) throws URISyntaxException {
        log.debug("REST request to update UserResource : {}", userResource);
        if (userResource.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (userResource.getUserBelongsTo() != null && userResource.getUserBelongsTo().getLogin().equalsIgnoreCase(request.getRemoteUser())) {
            UserResource result = userResourceService.save(userResource);
            return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, userResource.getId().toString()))
                .body(result);
        } else {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
    }

    /**
     * {@code GET  /user-resources} : get all the userResources.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of userResources in body.
     */
    @GetMapping("/user-resources")
    public List<UserResource> getAllUserResources(HttpServletRequest request) {
        log.debug("REST request to get all UserResources");
        String username = request.getRemoteUser();
        return userResourceService.findAll().stream().filter(userResource -> userResource.getUserBelongsTo() != null).filter(userResource -> userResource.getUserBelongsTo().getLogin().equalsIgnoreCase(username)).collect(Collectors.toList());
    }

    /**
     * {@code GET  /user-resources/:id} : get the "id" userResource.
     *
     * @param id the id of the userResource to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the userResource, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/user-resources/{id}")
    public ResponseEntity<UserResource> getUserResource(@PathVariable Long id, HttpServletRequest request) {
        log.debug("REST request to get UserResource : {}", id);
        Optional<UserResource> userResource = userResourceService.findOne(id);
        String username = request.getRemoteUser();
        if (userResource.isPresent() && userResource.get().getUserBelongsTo().getLogin().equalsIgnoreCase(username)) {
            return ResponseEntity.ok().body(userResource.get());
        } else {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
    }

    /**
     * {@code DELETE  /user-resources/:id} : delete the "id" userResource.
     *
     * @param id the id of the userResource to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/user-resources/{id}")
    public ResponseEntity<Void> deleteUserResource(@PathVariable Long id, HttpServletRequest request) {
        log.debug("REST request to delete UserResource : {}", id);
        String username = request.getRemoteUser();
        Optional<UserResource> userResource = userResourceService.findOne(id);
        if (userResource.isPresent() && userResource.get().getUserBelongsTo() != null && userResource.get().getUserBelongsTo().getLogin().equalsIgnoreCase(username)) {
            userResourceService.delete(id);
            return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString())).build();
        } else {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
    }
}

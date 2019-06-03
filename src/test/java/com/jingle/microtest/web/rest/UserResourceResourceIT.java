package com.jingle.microtest.web.rest;

import com.jingle.microtest.MicrotestApp;
import com.jingle.microtest.domain.User;
import com.jingle.microtest.domain.UserResource;
import com.jingle.microtest.repository.UserRepository;
import com.jingle.microtest.repository.UserResourceRepository;
import com.jingle.microtest.security.jwt.TokenProvider;
import com.jingle.microtest.service.UserResourceService;
import com.jingle.microtest.web.rest.errors.ExceptionTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static com.jingle.microtest.web.rest.TestUtil.createFormattingConversionService;
import static com.jingle.microtest.web.rest.TestUtil.sameInstant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@Link UserResourceResource} REST controller.
 */
@SpringBootTest(classes = MicrotestApp.class)
public class UserResourceResourceIT {

    private static final String DEFAULT_VALUE = "AAAAAAAAAA";
    private static final String UPDATED_VALUE = "BBBBBBBBBB";

    private static final ZonedDateTime DEFAULT_CREATED_AT = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_CREATED_AT = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    @Autowired
    private UserResourceRepository userResourceRepository;

    @Autowired
    private UserResourceService userResourceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Validator validator;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private AuthenticationManagerBuilder authenticationManager;

    private MockMvc restUserResourceMockMvc;

    private MockMvc mockMvc;

    private UserResource userResource;

    private String accessToken;

    private User user;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final UserResourceResource userResourceResource = new UserResourceResource(userResourceService, userRepository);
        this.restUserResourceMockMvc = MockMvcBuilders.standaloneSetup(userResourceResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
        UserJWTController userJWTController = new UserJWTController(tokenProvider, authenticationManager);

        this.mockMvc = MockMvcBuilders.standaloneSetup(userJWTController)
            .setControllerAdvice(exceptionTranslator)
            .build();

    }

    private void prepareUser() throws Exception {
//        userRepository.saveAndFlush(this.user);

        String response = mockMvc.perform(post("/api/authenticate")
            .content("{ \"username\": \"admin\", \"password\": \"admin\" }")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)).andReturn().getResponse().getContentAsString();
        this.accessToken = new JacksonJsonParser().parseMap(response).get("id_token").toString();
    }

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public UserResource createEntity(EntityManager em) {
        UserResource userResource = new UserResource()
            .value(DEFAULT_VALUE)
            .createdAt(DEFAULT_CREATED_AT).userBelongsTo(this.user);
        return userResource;
    }

    public User createUserEntity(EntityManager em) {
        return this.userRepository.findOneByLogin("admin").get();
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserResource createUpdatedEntity(EntityManager em) {
        UserResource userResource = new UserResource()
            .value(UPDATED_VALUE)
            .createdAt(UPDATED_CREATED_AT);
        return userResource;
    }

    @BeforeEach
    public void initTest() {
        user = createUserEntity(em);
        try {
            prepareUser();
        } catch (Exception e) {
            e.printStackTrace();
        }
        userResource = createEntity(em);
    }

    @Test
    @Transactional
    public void createUserResource() throws Exception {
        int databaseSizeBeforeCreate = userResourceRepository.findAll().size();

        // Create the UserResource
        restUserResourceMockMvc.perform(post("/api/user-resources").header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            )
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userResource)))
            .andExpect(status().isCreated());

        // Validate the UserResource in the database
        List<UserResource> userResourceList = userResourceRepository.findAll();
        assertThat(userResourceList).hasSize(databaseSizeBeforeCreate + 1);
        UserResource testUserResource = userResourceList.get(userResourceList.size() - 1);
        assertThat(testUserResource.getValue()).isEqualTo(DEFAULT_VALUE);
        assertThat(testUserResource.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
    }

    @Test
    @Transactional
    public void createUserResourceWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userResourceRepository.findAll().size();

        // Create the UserResource with an existing ID
        userResource.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserResourceMockMvc.perform(post("/api/user-resources")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userResource)))
            .andExpect(status().isBadRequest());

        // Validate the UserResource in the database
        List<UserResource> userResourceList = userResourceRepository.findAll();
        assertThat(userResourceList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkValueIsRequired() throws Exception {
        int databaseSizeBeforeTest = userResourceRepository.findAll().size();
        // set the field null
        userResource.setValue(null);

        // Create the UserResource, which fails.

        restUserResourceMockMvc.perform(post("/api/user-resources")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userResource)))
            .andExpect(status().isBadRequest());

        List<UserResource> userResourceList = userResourceRepository.findAll();
        assertThat(userResourceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = userResourceRepository.findAll().size();
        // set the field null
        userResource.setCreatedAt(null);

        // Create the UserResource, which fails.

        restUserResourceMockMvc.perform(post("/api/user-resources")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userResource)))
            .andExpect(status().isBadRequest());

        List<UserResource> userResourceList = userResourceRepository.findAll();
        assertThat(userResourceList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllUserResources() throws Exception {
        userResourceRepository.saveAndFlush(userResource);
        // Get all the userResourceList
        restUserResourceMockMvc.perform(get("/api/user-resources").header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            ))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(userResource.getId().intValue())))
            .andExpect(jsonPath("$.[*].value").value(hasItem(DEFAULT_VALUE.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(sameInstant(DEFAULT_CREATED_AT))));
    }

    @Test
    @Transactional
    public void getUserResource() throws Exception {
        // Initialize the database
        userResourceRepository.saveAndFlush(userResource);

        // Get the userResource
        restUserResourceMockMvc.perform(get("/api/user-resources/{id}", userResource.getId()).header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            ))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(userResource.getId().intValue()))
            .andExpect(jsonPath("$.value").value(DEFAULT_VALUE.toString()))
            .andExpect(jsonPath("$.createdAt").value(sameInstant(DEFAULT_CREATED_AT)));
    }

    @Test
    @Transactional
    public void getNonExistingUserResource() throws Exception {
        // Get the userResource
        restUserResourceMockMvc.perform(get("/api/user-resources/{id}", Long.MAX_VALUE).header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            ))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @Transactional
    public void updateUserResource() throws Exception {
        // Initialize the database
        userResourceService.save(userResource);

        int databaseSizeBeforeUpdate = userResourceRepository.findAll().size();

        // Update the userResource
        UserResource updatedUserResource = userResourceRepository.findById(userResource.getId()).get();
        // Disconnect from session so that the updates on updatedUserResource are not directly saved in db
        em.detach(updatedUserResource);
        updatedUserResource
            .value(UPDATED_VALUE)
            .createdAt(UPDATED_CREATED_AT)
            .setUserBelongsTo(this.user);

        restUserResourceMockMvc.perform(put("/api/user-resources").header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            )
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedUserResource)))
            .andExpect(status().isOk());

        // Validate the UserResource in the database
        List<UserResource> userResourceList = userResourceRepository.findAll();
        assertThat(userResourceList).hasSize(databaseSizeBeforeUpdate);
        UserResource testUserResource = userResourceList.get(userResourceList.size() - 1);
        assertThat(testUserResource.getValue()).isEqualTo(UPDATED_VALUE);
        assertThat(testUserResource.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    public void updateNonExistingUserResource() throws Exception {
        int databaseSizeBeforeUpdate = userResourceRepository.findAll().size();

        // Create the UserResource

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserResourceMockMvc.perform(put("/api/user-resources")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userResource)))
            .andExpect(status().isBadRequest());

        // Validate the UserResource in the database
        List<UserResource> userResourceList = userResourceRepository.findAll();
        assertThat(userResourceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteUserResource() throws Exception {
        // Initialize the database
        userResourceService.save(userResource);

        int databaseSizeBeforeDelete = userResourceRepository.findAll().size();

        // Delete the userResource
        restUserResourceMockMvc.perform(delete("/api/user-resources/{id}", userResource.getId()).header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            )
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database is empty
        List<UserResource> userResourceList = userResourceRepository.findAll();
        assertThat(userResourceList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserResource.class);
        UserResource userResource1 = new UserResource();
        userResource1.setId(1L);
        UserResource userResource2 = new UserResource();
        userResource2.setId(userResource1.getId());
        assertThat(userResource1).isEqualTo(userResource2);
        userResource2.setId(2L);
        assertThat(userResource1).isNotEqualTo(userResource2);
        userResource1.setId(null);
        assertThat(userResource1).isNotEqualTo(userResource2);
    }
}

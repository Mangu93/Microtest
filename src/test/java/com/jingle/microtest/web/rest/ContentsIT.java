package com.jingle.microtest.web.rest;

import com.jingle.microtest.MicrotestApp;
import com.jingle.microtest.domain.Contents;
import com.jingle.microtest.domain.User;
import com.jingle.microtest.repository.UserRepository;
import com.jingle.microtest.repository.ContentResourceRepository;
import com.jingle.microtest.security.jwt.TokenProvider;
import com.jingle.microtest.service.ContentResourceService;
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
 * Integration tests for the {@link ContentResource} REST controller.
 */
@SpringBootTest(classes = MicrotestApp.class)
public class ContentsIT {

    private static final String DEFAULT_VALUE = "AAAAAAAAAA";
    private static final String UPDATED_VALUE = "BBBBBBBBBB";

    private static final ZonedDateTime DEFAULT_CREATED_AT = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_CREATED_AT = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    @Autowired
    private ContentResourceRepository contentResourceRepository;

    @Autowired
    private ContentResourceService contentResourceService;

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

    private MockMvc restContentMockMvc;

    private MockMvc mockMvc;

    private Contents contents;

    private String accessToken;

    private User user;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        final ContentResource contentResource = new ContentResource(contentResourceService, userRepository);
        this.restContentMockMvc = MockMvcBuilders.standaloneSetup(contentResource)
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
    Contents createEntity(EntityManager em) {
        return new Contents()
            .value(DEFAULT_VALUE)
            .createdAt(DEFAULT_CREATED_AT).userBelongsTo(this.user);
    }

    User createUserEntity(EntityManager em) {
        return this.userRepository.findOneByLogin("admin").get();
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    static Contents createUpdatedEntity(EntityManager em) {
        return new Contents()
            .value(UPDATED_VALUE)
            .createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        user = createUserEntity(em);
        try {
            prepareUser();
        } catch (Exception e) {
            e.printStackTrace();
        }
        contents = createEntity(em);
    }

    @Test
    @Transactional
    void createContent() throws Exception {
        int databaseSizeBeforeCreate = contentResourceRepository.findAll().size();

        // Create the Contents
        restContentMockMvc.perform(post("/api/contents").header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            )
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contents)))
            .andExpect(status().isCreated());

        // Validate the Contents in the database
        List<Contents> contentsList = contentResourceRepository.findAll();
        assertThat(contentsList).hasSize(databaseSizeBeforeCreate + 1);
        Contents testContents = contentsList.get(contentsList.size() - 1);
        assertThat(testContents.getValue()).isEqualTo(DEFAULT_VALUE);
        assertThat(testContents.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
    }

    @Test
    @Transactional
    void createContentResourceWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = contentResourceRepository.findAll().size();

        // Create the Contents with an existing ID
        contents.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restContentMockMvc.perform(post("/api/contents")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contents)))
            .andExpect(status().isBadRequest());

        // Validate the Contents in the database
        List<Contents> contentsList = contentResourceRepository.findAll();
        assertThat(contentsList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    void checkValueIsRequired() throws Exception {
        int databaseSizeBeforeTest = contentResourceRepository.findAll().size();
        // set the field null
        contents.setValue(null);

        // Create the Contents, which fails.

        restContentMockMvc.perform(post("/api/contents")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contents)))
            .andExpect(status().isBadRequest());

        List<Contents> contentsList = contentResourceRepository.findAll();
        assertThat(contentsList).hasSize(databaseSizeBeforeTest);
    }


    @Test
    @Transactional
    void getAllContents() throws Exception {
        contentResourceRepository.saveAndFlush(contents);
        // Get all the content
        restContentMockMvc.perform(get("/api/contents").header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            ))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(contents.getId().intValue())))
            .andExpect(jsonPath("$.[*].value").value(hasItem(DEFAULT_VALUE)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(sameInstant(DEFAULT_CREATED_AT))));
    }

    @Test
    @Transactional
    void getContent() throws Exception {
        // Initialize the database
        contentResourceRepository.saveAndFlush(contents);

        // Get the contents
        restContentMockMvc.perform(get("/api/contents/{id}", contents.getId()).header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            ))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(contents.getId().intValue()))
            .andExpect(jsonPath("$.value").value(DEFAULT_VALUE))
            .andExpect(jsonPath("$.createdAt").value(sameInstant(DEFAULT_CREATED_AT)));
    }

    @Test
    @Transactional
    void getNonExistingContent() throws Exception {
        // Get the contents
        restContentMockMvc.perform(get("/api/contents/{id}", Long.MAX_VALUE).header("Authorization", "Bearer " + accessToken)
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
    void updateContent() throws Exception {
        // Initialize the database
        contentResourceService.save(contents);

        int databaseSizeBeforeUpdate = contentResourceRepository.findAll().size();

        // Update the contents
        Contents updatedContents = contentResourceRepository.findById(contents.getId()).get();
        // Disconnect from session so that the updates on updatedContents are not directly saved in db
        em.detach(updatedContents);
        updatedContents
            .value(UPDATED_VALUE)
            .createdAt(UPDATED_CREATED_AT)
            .setUserBelongsTo(this.user);

        restContentMockMvc.perform(put("/api/contents").header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            )
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedContents)))
            .andExpect(status().isOk());

        // Validate the Contents in the database
        List<Contents> contentsList = contentResourceRepository.findAll();
        assertThat(contentsList).hasSize(databaseSizeBeforeUpdate);
        Contents testContents = contentsList.get(contentsList.size() - 1);
        assertThat(testContents.getValue()).isEqualTo(UPDATED_VALUE);
        assertThat(testContents.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void updateNonExistingContent() throws Exception {
        int databaseSizeBeforeUpdate = contentResourceRepository.findAll().size();

        // Create the Contents

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restContentMockMvc.perform(put("/api/contents")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contents)))
            .andExpect(status().isBadRequest());

        // Validate the Contents in the database
        List<Contents> contentsList = contentResourceRepository.findAll();
        assertThat(contentsList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteContent() throws Exception {
        // Initialize the database
        contentResourceService.save(contents);

        int databaseSizeBeforeDelete = contentResourceRepository.findAll().size();

        // Delete the contents
        restContentMockMvc.perform(delete("/api/contents/{id}", contents.getId()).header("Authorization", "Bearer " + accessToken)
            .with(
                request -> {
                    request.setRemoteUser(this.user.getLogin());
                    return request;
                }
            )
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database is empty
        List<Contents> contentsList = contentResourceRepository.findAll();
        assertThat(contentsList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Contents.class);
        Contents contents1 = new Contents();
        contents1.setId(1L);
        Contents contents2 = new Contents();
        contents2.setId(contents1.getId());
        assertThat(contents1).isEqualTo(contents2);
        contents2.setId(2L);
        assertThat(contents1).isNotEqualTo(contents2);
        contents1.setId(null);
        assertThat(contents1).isNotEqualTo(contents2);
    }
}

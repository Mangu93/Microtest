package com.jingle.microtest.web.rest;


import com.jingle.microtest.domain.User;
import com.jingle.microtest.repository.UserRepository;
import com.jingle.microtest.security.SecurityUtils;
import com.jingle.microtest.service.MailService;
import com.jingle.microtest.service.UserService;
import com.jingle.microtest.service.dto.PasswordChangeDTO;
import com.jingle.microtest.service.dto.UserDTO;
import com.jingle.microtest.web.rest.errors.EmailAlreadyUsedException;
import com.jingle.microtest.web.rest.errors.EmailNotFoundException;
import com.jingle.microtest.web.rest.errors.InvalidPasswordException;
import com.jingle.microtest.web.rest.errors.LoginAlreadyUsedException;
import com.jingle.microtest.web.rest.vm.KeyAndPasswordVM;
import com.jingle.microtest.web.rest.vm.LoginVM;
import com.jingle.microtest.web.rest.vm.ManagedUserVM;
import io.github.jhipster.web.util.HeaderUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private static class AccountResourceException extends RuntimeException {
        private AccountResourceException(String message) {
            super(message);
        }
    }

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    private final UserRepository userRepository;

    private final UserService userService;

    private final MailService mailService;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    public AccountResource(UserRepository userRepository, UserService userService, MailService mailService, AuthenticationManagerBuilder authenticationManagerBuilder) {

        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
    }

    /**
     * {@code POST  /register} : register the user.
     *
     * @param managedUserVM the managed user View Model.
     * @throws InvalidPasswordException  {@code 400 (Bad Request)} if the password is incorrect.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already used.
     * @throws EmailNotFoundException    {@code 400 (Bad Request)} if the email is not present.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        if (!checkPasswordLength(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerUser(managedUserVM, managedUserVM.getPassword());
        if (user == null) {
            throw new EmailNotFoundException();
        }
        mailService.sendActivationEmail(user);
    }

    /**
     * {@code POST  /register} : register the service.
     *
     * @param managedUserVM the managed user View Model.
     * @throws InvalidPasswordException  {@code 400 (Bad Request)} if the password is incorrect.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already used.
     * @throws EmailNotFoundException    {@code 400 (Bad Request)} if the email is not present.
     */
    @PostMapping("/register-service")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerService(@Valid @RequestBody ManagedUserVM managedUserVM) {
        if (!checkPasswordLength(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerService(managedUserVM, managedUserVM.getPassword());
        if (user == null) {
            throw new EmailNotFoundException();
        }
        mailService.sendActivationEmail(user);
    }

    /**
     * {@code GET  /authenticate} : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request.
     * @return the login if the user is authenticated.
     */
    @GetMapping("/authenticate")
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * {@code GET  /account} : get the current user.
     *
     * @return the current user.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user couldn't be returned.
     */
    @GetMapping("/account")
    public UserDTO getAccount() {
        return userService.getUserWithAuthorities()
            .map(UserDTO::new)
            .orElseThrow(() -> new AccountResourceException("User could not be found"));
    }

    /**
     * {@code DELETE /account}: delete the account
     *
     * @param request {@link HttpServletRequest}
     * @param loginVM Logged-in user
     * @return 201 code for success
     */
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(HttpServletRequest request, @Valid @RequestBody LoginVM loginVM) {
        if (request.getRemoteUser().equalsIgnoreCase(loginVM.getUsername())) {
            try {
                UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());

                Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
                if (authentication != null) {
                    userService.deleteUser(loginVM.getUsername());
                    return ResponseEntity.noContent().headers(HeaderUtil.createAlert(applicationName, "A user is deleted with identifier " + loginVM.getUsername(), loginVM.getUsername())).build();
                }
            } catch (AuthenticationException ex) {
                throw new AccountResourceException("User and password do not match");
            }
        }
        throw new AccountResourceException("Authentication needed to do this operation");
    }

    /**
     * {@code DELETE /service}: delete the service
     *
     * @param request {@link HttpServletRequest}
     * @param loginVM Logged-in service
     * @return 201 code for success
     */
    @DeleteMapping("/service")
    public ResponseEntity<Void> deleteService(HttpServletRequest request, @Valid @RequestBody LoginVM loginVM) {
        if (request.getRemoteUser().equalsIgnoreCase(loginVM.getUsername())) {
            try {
                UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());

                Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
                if (authentication != null) {
                    userService.deleteUser(loginVM.getUsername());
                    return ResponseEntity.noContent().headers(HeaderUtil.createAlert(applicationName, "A service is deleted with identifier " + loginVM.getUsername(), loginVM.getUsername())).build();
                }
            } catch (AuthenticationException ex) {
                throw new AccountResourceException("User and password do not match");
            }
        }
        throw new AccountResourceException("Authentication needed to do this operation");
    }

    /**
     * {@code POST  /account} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws RuntimeException          {@code 500 (Internal Server Error)} if the user login wasn't found.
     */
    @PostMapping("/account")
    public void saveAccount(@Valid @RequestBody UserDTO userDTO) {
        Optional<User> user = getUser(userDTO.getEmail());
        if (!user.isPresent()) {
            throw new AccountResourceException("User could not be found");
        }
        userService.updateUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(),
            userDTO.getLangKey(), userDTO.getImageUrl());
    }
    /**
     * {@code POST  /service} : update the current service information.
     *
     * @param managedUserVM the current service information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws RuntimeException          {@code 500 (Internal Server Error)} if the service login wasn't found.
     */
    @PostMapping("/service")
    public void saveService(@Valid @RequestBody ManagedUserVM managedUserVM) {
        Optional<User> user = getUser(managedUserVM.getEmail());
        if (!user.isPresent()) {
            throw new AccountResourceException("Service could not be found");
        }
        userService.updateUser(managedUserVM.getFirstName(), managedUserVM.getLastName(), managedUserVM.getEmail(),
            managedUserVM.getLangKey(), managedUserVM.getImageUrl());
    }

    private Optional<User> getUser(String email) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new AccountResourceException("Current user login not found"));
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(email);
        if (existingUser.isPresent() && (!existingUser.get().getLogin().equalsIgnoreCase(userLogin))) {
            throw new EmailAlreadyUsedException();
        }
        return userRepository.findOneByLogin(userLogin);
    }

    /**
     * {@code POST  /account/change-password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/account/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (!checkPasswordLength(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    /**
     * {@code POST   /account/reset-password/init} : Send an email to reset the password of the user.
     *
     * @param mail the mail of the user.
     * @throws EmailNotFoundException {@code 400 (Bad Request)} if the email address is not registered.
     */
    @PostMapping(path = "/account/reset-password/init")
    public void requestPasswordReset(@RequestBody String mail) {
        mailService.sendPasswordResetMail(
            userService.requestPasswordReset(mail)
                .orElseThrow(EmailNotFoundException::new)
        );
    }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal Server Error)} if the password could not be reset.
     */
    @PostMapping(path = "/account/reset-password/finish")
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user =
            userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this reset key");
        }
    }

    private static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) &&
            password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH &&
            password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH;
    }
}

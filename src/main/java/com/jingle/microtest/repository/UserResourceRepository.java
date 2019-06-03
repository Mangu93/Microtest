package com.jingle.microtest.repository;

import com.jingle.microtest.domain.UserResource;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data  repository for the UserResource entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserResourceRepository extends JpaRepository<UserResource, Long> {

    @Query("select userResource from UserResource userResource where userResource.userBelongsTo.login = ?#{principal.username}")
    List<UserResource> findByUserBelongsToIsCurrentUser();

}

package com.jingle.microtest.repository;

import com.jingle.microtest.domain.Contents;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data  repository for the Contents entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ContentResourceRepository extends JpaRepository<Contents, Long> {

    @Query("select userResource from Contents userResource where userResource.userBelongsTo.login = ?#{principal.username}")
    List<Contents> findByUserBelongsToIsCurrentUser();

}

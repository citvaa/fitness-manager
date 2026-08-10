package com.example.demo.repository.user;

import com.example.demo.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByRegistrationKey(String registrationKey);

    Optional<User> findByEmail(String username);

    Optional<User> findByResetKey(String token);

    Page<User> findByEmailContaining(String username, Pageable pageable);

    /** Bulk JPQL delete - used by UserServiceImpl.delete() instead of the inherited entity-level
     * delete(User). User.userRoles is EAGER-fetched with cascade=ALL/orphanRemoval=true, so by
     * the time delete() runs, the persistence context already holds that collection from the
     * earlier findById() lookup; entity-level delete cascades over it and hits the pre-existing
     * BaseEntity id-less equals()/hashCode() bug (see AGENTS.md "Known issues"). A bulk delete
     * never touches Java object equality, so it sidesteps that bug entirely. */
    @Modifying
    @Query("DELETE FROM User u WHERE u = :user")
    void deleteUser(@Param("user") User user);
}

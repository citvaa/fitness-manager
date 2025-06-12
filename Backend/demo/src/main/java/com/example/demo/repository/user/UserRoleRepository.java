package com.example.demo.repository.user;

import com.example.demo.model.user.User;
import com.example.demo.model.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    @Modifying
    @Query("DELETE FROM UserRole u WHERE u.user = :user")
    void deleteByUser(@Param("user") User user);
}

package com.example.demo.repository;

import com.example.demo.model.Client;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    @Modifying
    @Query("DELETE FROM Client c WHERE c.user = :user")
    void deleteByUser(@Param("user") User user);

    Optional<Client> findByUserEmail(String userMail);
}

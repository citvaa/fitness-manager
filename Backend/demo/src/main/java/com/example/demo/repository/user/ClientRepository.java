package com.example.demo.repository.user;

import com.example.demo.model.user.Client;
import com.example.demo.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    @Modifying
    @Query("DELETE FROM Client c WHERE c.user = :user")
    void deleteByUser(@Param("user") User user);

    Optional<Client> findByUserEmail(String userMail);
    Optional<Client> findByUserId(Integer userId);

    @Query("select distinct ca.client from ClientAppointment ca where ca.appointment.trainer.id = :trainerId")
    List<Client> findDistinctTrainedBy(@Param("trainerId") Integer trainerId);
}



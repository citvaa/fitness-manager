package com.example.demo.repository;

import com.example.demo.model.Payment;
import com.example.demo.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Modifying
    @Query("DELETE FROM Payment p WHERE p.client.user = :user")
    void deleteByUser(@Param("user") User user);
}

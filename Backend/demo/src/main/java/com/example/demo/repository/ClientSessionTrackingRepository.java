package com.example.demo.repository;

import com.example.demo.model.Client;
import com.example.demo.model.ClientSessionTracking;
import com.example.demo.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientSessionTrackingRepository extends JpaRepository<ClientSessionTracking, Integer> {
    Optional<ClientSessionTracking> findByClientAndSession(Client client, Session session);
}

package com.example.demo.repository.user;

import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientSessionTracking;
import com.example.demo.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientSessionTrackingRepository extends JpaRepository<ClientSessionTracking, Integer> {
    Optional<ClientSessionTracking> findByClientAndSession(Client client, Session session);

    /** Bulk JPQL delete rather than loading+deleting entities - see UserServiceImpl.delete()
     * for why (BaseEntity's id-less equals()/hashCode() corrupts Hibernate's cascade traversal
     * over these Set-typed collections; a bulk delete never touches Java object equality). */
    @Modifying
    @Query("DELETE FROM ClientSessionTracking cst WHERE cst.client = :client")
    void deleteByClient(@Param("client") Client client);
}

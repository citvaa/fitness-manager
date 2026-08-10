package com.example.demo.repository.progress;

import com.example.demo.model.progress.ClientProgressEntry;
import com.example.demo.model.user.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientProgressEntryRepository extends JpaRepository<ClientProgressEntry, Integer> {

    List<ClientProgressEntry> findByClientIdOrderByEntryDateAsc(Integer clientId);

    /** Bulk JPQL delete - used before deleting a Client (see UserServiceImpl.delete()); this
     * table is not one of Client's cascade=ALL collections and would otherwise block the Client
     * row with an FK violation. */
    @Modifying
    @Query("DELETE FROM ClientProgressEntry e WHERE e.client = :client")
    void deleteByClient(@Param("client") Client client);
}

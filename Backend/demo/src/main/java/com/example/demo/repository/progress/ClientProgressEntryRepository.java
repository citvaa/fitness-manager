package com.example.demo.repository.progress;

import com.example.demo.model.progress.ClientProgressEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientProgressEntryRepository extends JpaRepository<ClientProgressEntry, Integer> {
    List<ClientProgressEntry> findByClientIdOrderByEntryDateAsc(Integer clientId);
}

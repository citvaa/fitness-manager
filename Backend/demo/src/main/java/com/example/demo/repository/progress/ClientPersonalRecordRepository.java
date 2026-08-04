package com.example.demo.repository.progress;

import com.example.demo.model.progress.ClientPersonalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientPersonalRecordRepository extends JpaRepository<ClientPersonalRecord, Integer> {

    List<ClientPersonalRecord> findByClientIdOrderByRecordDateDesc(Integer clientId);

    List<ClientPersonalRecord> findByClientIdAndExerciseNameIgnoreCaseOrderByRecordDateDesc(Integer clientId, String exerciseName);
}

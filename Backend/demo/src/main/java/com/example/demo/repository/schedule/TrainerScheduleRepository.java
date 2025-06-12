package com.example.demo.repository.schedule;

import com.example.demo.model.user.Trainer;
import com.example.demo.model.schedule.TrainerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TrainerScheduleRepository extends JpaRepository<TrainerSchedule, Integer> {

    @Query("""
        SELECT COUNT(ts) > 0 FROM TrainerSchedule ts
        WHERE ts.trainer.id = :trainerId
        AND ts.date = :date
        AND (
            (:startTime BETWEEN ts.startTime AND ts.endTime)
            OR (:endTime BETWEEN ts.startTime AND ts.endTime)
            OR (ts.startTime BETWEEN :startTime AND :endTime)
        )
   """)
    boolean existsByTrainerIdAndDateAndTimeRange(@Param("trainerId") Integer trainerId,
                                                 @Param("date") LocalDate date,
                                                 @Param("startTime") LocalTime startTime,
                                                 @Param("endTime") LocalTime endTime);


    @Modifying
    @Query("DELETE FROM TrainerSchedule t WHERE t.trainer = :trainer")
    void deleteByTrainer(@Param("trainer") Trainer trainer);

    List<TrainerSchedule> findByTrainerIdAndDate(Integer trainerId, LocalDate date);
}

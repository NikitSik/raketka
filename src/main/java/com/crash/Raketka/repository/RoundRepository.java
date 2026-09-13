package com.crash.Raketka.repository;

import com.crash.Raketka.domain.Round;
import com.crash.Raketka.domain.RoundStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {

    List<Round> findByStatus(RoundStatus status);

    List<Round> findByUserIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(Long userId, Pageable pageable);

    Optional<Round> findByIdAndUserId(Long id, Long userId);
}
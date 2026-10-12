package com.capstone.be.domain.reading_record.repository;

import com.capstone.be.domain.reading_record.entity.ReadingRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingRecordRepository extends JpaRepository<ReadingRecord, Long> {

    Optional<ReadingRecord> findByUserIdAndBookId(Long userId, Long bookId);
}

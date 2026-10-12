package com.capstone.be.domain.reading_record.enums;

/**
 * "내 도서 목록 보기"(GET /api/v1/reading-records) 조회 필터.
 * ALL: 전체 (기본값)
 * COMPLETED: 완독한 책만 - 완독 기준은 progressRate(진행률)가 100(완료 상태)인 경우.
 */
public enum ReadingRecordStatus {
    ALL,
    COMPLETED
}

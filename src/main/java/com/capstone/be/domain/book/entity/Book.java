package com.capstone.be.domain.book.entity;

import com.capstone.be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    // 책 소개/설명. (V3 마이그레이션에서 오타였던 descrption -> description으로 컬럼명 수정함)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 장르/카테고리는 V16 마이그레이션에서 고정 목록(genres 테이블)으로 분리됐다.
    // 연관관계 대신 다른 도메인들과 동일하게 raw FK로 둔다(GenreRepository로 직접 조회).
    @Column(name = "genre_id")
    private Long genreId;

    // 출판사
    @Column(name = "publisher", nullable = false, length = 100)
    private String publisher;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Builder
    public Book(String title, String author, String coverImageUrl, String description, Long genreId, String publisher, LocalDate createdAt) {
        this.title = title;
        this.author = author;
        this.coverImageUrl = coverImageUrl;
        this.description = description;
        this.genreId = genreId;
        this.publisher = publisher;
        this.createdAt = createdAt;
    }
}

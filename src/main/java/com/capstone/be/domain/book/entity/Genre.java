package com.capstone.be.domain.book.entity;

import com.capstone.be.domain.book.enums.GenreType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 책 장르/카테고리 고정 목록 하나. type(대분류) 하나에 value(세부 분류)가 여러 개 있을 수 있는
 * 구조다(예: type=소설, value=한국소설/외국소설/추리/...). Book.genreId가 이 테이블의 genre_id를
 * 가리킨다 - 초기 데이터는 V17__create_genres_table.sql 참고.
 */
@Entity
@Table(name = "genres")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id")
    private Long genreId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private GenreType type;

    @Column(name = "value", nullable = false, length = 100)
    private String value;

    @Builder
    public Genre(GenreType type, String value) {
        this.type = type;
        this.value = value;
    }
}

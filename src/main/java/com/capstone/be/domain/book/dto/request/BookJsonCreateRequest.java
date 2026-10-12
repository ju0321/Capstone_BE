package com.capstone.be.domain.book.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * "JSON형태 책 저장하기" API (POST /api/v1/books/json)의 책 메타데이터 부분.
 * multipart 요청 안에서 "book"이라는 이름의 파트로 JSON(Content-Type: application/json)
 * 형태로 전달되며, 표지 이미지/book_Json 파일과 달리 파일이 아니라서 이 DTO는 Jackson이
 * 직접 역직렬화한다 (그래서 스프링 폼 바인딩용 @Setter 대신 @JsonProperty를 붙임).
 * 표지 이미지/book_Json 파일은 컨트롤러에서 별도 MultipartFile 파라미터로 받는다.
 */
@Getter
@NoArgsConstructor
public class BookJsonCreateRequest {

    @NotBlank(message = "책 제목을 입력해주세요.")
    @Size(max = 255, message = "책 제목은 255자를 넘을 수 없습니다.")
    @JsonProperty("title")
    private String title;

    @NotBlank(message = "저자를 입력해주세요.")
    @Size(max = 100, message = "저자는 100자를 넘을 수 없습니다.")
    @JsonProperty("author")
    private String author;

    @NotBlank(message = "출판사를 입력해주세요.")
    @Size(max = 100, message = "출판사는 100자를 넘을 수 없습니다.")
    @JsonProperty("publisher")
    private String publisher;

    // 책 소개/설명. 필수 아님 (설명이 없는 책도 있을 수 있음)
    @JsonProperty("description")
    private String description;

    // 장르(genres.genre_id). 필수 아님 - 비워두면 장르 없이 등록되고, 나중에 채워 넣을 수 있다.
    // 어떤 genreId가 있는지는 "GET /api/v1/books/genres"(관리자용)로 확인한다.
    @JsonProperty("genreId")
    private Long genreId;

    @NotNull(message = "출판일을 입력해주세요.")
    @JsonProperty("createdAt")
    private LocalDate createdAt;
}

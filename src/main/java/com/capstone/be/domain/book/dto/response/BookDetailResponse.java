package com.capstone.be.domain.book.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class BookDetailResponse {

    private String title;
    private String author;
    private String coverImageUrl;
    private String description;
    private List<GenreResponse> genreList;
    private String publisher;
    private LocalDate createdAt;
}

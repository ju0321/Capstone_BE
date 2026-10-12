package com.capstone.be.domain.book.dto.pdf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * book_Json 파일 안의 페이지 하나를 그대로 받는 DTO.
 * (yeobaek-python의 app/schemas/pdf.py::PageContent와 1:1로 대응)
 */
@Getter
@NoArgsConstructor
public class BookJsonPageDto {

    @JsonProperty("page_number")
    private Integer pageNumber;

    @JsonProperty("chapter")
    private String chapter;

    @JsonProperty("sentences")
    private List<String> sentences;

    @JsonProperty("images")
    private List<BookJsonImageDto> images;
}

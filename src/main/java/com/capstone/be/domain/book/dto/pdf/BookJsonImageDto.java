package com.capstone.be.domain.book.dto.pdf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI(yeobaek-python)가 만든 book_Json 파일 안의 이미지 정보를 그대로 받는 DTO.
 * (yeobaek-python의 app/schemas/pdf.py::ImageInfo와 1:1로 대응)
 */
@Getter
@NoArgsConstructor
public class BookJsonImageDto {

    @JsonProperty("order")
    private Integer order;

    @JsonProperty("filename")
    private String filename;
}

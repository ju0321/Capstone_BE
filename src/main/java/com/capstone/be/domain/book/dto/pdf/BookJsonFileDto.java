package com.capstone.be.domain.book.dto.pdf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FastAPI(yeobaek-python)가 PDF를 파싱해서 book_Json/ 폴더에 저장해둔 결과 파일 전체 구조.
 * (yeobaek-python의 app/schemas/pdf.py::PdfParseResult와 1:1로 대응)
 */
@Getter
@NoArgsConstructor
public class BookJsonFileDto {

    @JsonProperty("title")
    private String title;

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("pages")
    private List<BookJsonPageDto> pages;
}

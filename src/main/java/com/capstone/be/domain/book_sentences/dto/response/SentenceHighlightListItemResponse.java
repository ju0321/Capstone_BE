package com.capstone.be.domain.book_sentences.dto.response;

import com.capstone.be.domain.book_sentences.enums.HighlightColor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "수집한 문장 목록 항목")
public class SentenceHighlightListItemResponse {

    @Schema(description = "문장 ID", example = "1")
    private Long sentenceId;

    @Schema(description = "수집한 문장 원문", example = "○ 천 년 후에도 사람이 존재할까?")
    private String content;

    @Schema(description = "형광펜 색상", example = "YELLOW")
    private HighlightColor color;

    @Schema(description = "수집한 시각")
    private LocalDateTime createdAt;

    @Schema(description = "책 ID", example = "1")
    private Long bookId;

    @Schema(description = "책 제목", example = "구의 증명")
    private String bookTitle;

    @Schema(description = "문장이 속한 페이지 ID", example = "3")
    private Long pageId;

    @Schema(description = "페이지 번호", example = "3")
    private Integer pageNumber;

    @Schema(description = "챕터명", example = "구의 증명")
    private String chapter;
}

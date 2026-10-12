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
@Schema(description = "문장 수집(하이라이트) 응답")
public class SentenceHighlightResponse {

    @Schema(description = "문장 ID", example = "1")
    private Long sentenceId;

    @Schema(description = "수집한 문장 원문", example = "○ 천 년 후에도 사람이 존재할까?")
    private String content;

    @Schema(description = "형광펜 색상", example = "YELLOW")
    private HighlightColor color;

    @Schema(description = "수집한 시각")
    private LocalDateTime createdAt;
}

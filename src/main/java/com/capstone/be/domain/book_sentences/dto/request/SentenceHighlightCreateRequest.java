package com.capstone.be.domain.book_sentences.dto.request;

import com.capstone.be.domain.book_sentences.enums.HighlightColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "문장 수집(하이라이트 생성) 요청")
public class SentenceHighlightCreateRequest {

    @Schema(description = "수집할 문장의 ID", example = "1")
    @NotNull(message = "문장 ID는 필수입니다.")
    private Long sentenceId;

    @Schema(description = "드래그로 선택한 문장 원문", example = "○ 천 년 후에도 사람이 존재할까?")
    @NotBlank(message = "수집할 문장 내용은 필수입니다.")
    private String content;

    @Schema(description = "형광펜 색상", example = "YELLOW")
    @NotNull(message = "형광펜 색상은 필수입니다.")
    private HighlightColor color;
}

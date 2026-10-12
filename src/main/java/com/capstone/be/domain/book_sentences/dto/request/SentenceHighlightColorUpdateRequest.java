package com.capstone.be.domain.book_sentences.dto.request;

import com.capstone.be.domain.book_sentences.enums.HighlightColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "하이라이트 색상 변경 요청")
public class SentenceHighlightColorUpdateRequest {

    @Schema(description = "바꿀 형광펜 색상", example = "GREEN")
    @NotNull(message = "형광펜 색상은 필수입니다.")
    private HighlightColor color;
}

package com.capstone.be.domain.book_sentences.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "하이라이트 메모 작성/수정 요청")
public class SentenceHighlightMemoUpdateRequest {

    @Schema(description = "메모 내용 (비워서 보내면 메모가 삭제됨)", example = "이 문장 인상 깊었다")
    @Size(max = 1000, message = "메모는 1000자를 넘을 수 없습니다.")
    private String memo;
}

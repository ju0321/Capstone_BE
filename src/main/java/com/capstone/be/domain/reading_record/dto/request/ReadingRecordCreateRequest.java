package com.capstone.be.domain.reading_record.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "내 도서 추가 요청")
public class ReadingRecordCreateRequest {

    @NotNull(message = "bookId는 필수입니다.")
    @Schema(description = "추가할 책 ID", example = "1")
    private Long bookId;
}

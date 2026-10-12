package com.capstone.be.domain.reading_record.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "진행률 갱신 요청 - 페이지를 넘길 때마다 지금 보고 있는 페이지를 보냄")
public class ReadingRecordProgressUpdateRequest {

    @NotNull(message = "pageId는 필수입니다.")
    @Schema(description = "지금 보고 있는 페이지 ID", example = "42")
    private Long pageId;
}

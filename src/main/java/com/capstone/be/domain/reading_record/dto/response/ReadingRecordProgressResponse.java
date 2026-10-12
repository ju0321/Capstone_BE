package com.capstone.be.domain.reading_record.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "진행률 갱신 응답")
public class ReadingRecordProgressResponse {

    @Schema(description = "독서 기록 ID", example = "1")
    private Long recordId;

    @Schema(description = "현재 마지막 페이지 번호", example = "45")
    private Integer lastPageNumber;

    @Schema(description = "진행률(%). 정수(소수점 버림)로 저장됨", example = "45")
    private BigDecimal progressRate;

    @Schema(description = "마지막으로 읽은 시각")
    private LocalDateTime lastReadAt;
}

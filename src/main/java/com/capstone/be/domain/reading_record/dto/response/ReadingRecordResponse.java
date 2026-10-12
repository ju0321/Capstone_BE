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
@Schema(description = "완독 처리 응답")
public class ReadingRecordResponse {

    @Schema(description = "독서 기록 ID", example = "1")
    private Long recordId;

    @Schema(description = "책 ID", example = "1")
    private Long bookId;

    @Schema(description = "책 제목", example = "구의 증명")
    private String bookTitle;

    @Schema(description = "진행률(%)", example = "100.00")
    private BigDecimal progressRate;

    @Schema(description = "완독 시각(최초 완독 시점, 재호출해도 안 바뀜)")
    private LocalDateTime completedAt;
}

package com.capstone.be.domain.reading_record.dto.response;

import com.capstone.be.domain.reading_record.enums.CurrentBookStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "홈 화면 - 최근 읽은 책 카드")
public class CurrentReadingResponse {

    @Schema(description = "카드 상태 - READING(독서 진행 중)")
    private CurrentBookStatus status;

    @Schema(description = "독서 기록 ID", example = "1")
    private Long recordId;

    @Schema(description = "책 ID", example = "1")
    private Long bookId;

    @Schema(description = "책 제목", example = "프로젝트 헤일메리")
    private String bookTitle;

    @Schema(description = "책 표지 이미지 URL")
    private String coverImageUrl;

    @Schema(description = "진행률(%)", example = "50.00")
    private BigDecimal progressRate;

    @Schema(description = "남은 페이지 수", example = "150")
    private Integer remainingPages;
}

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
@Schema(description = "내 도서 목록 항목 하나")
public class ReadingRecordListItemResponse {

    @Schema(description = "독서 기록 ID", example = "1")
    private Long recordId;

    @Schema(description = "책 ID", example = "1")
    private Long bookId;

    @Schema(description = "책 제목", example = "구의 증명")
    private String bookTitle;

    @Schema(description = "저자", example = "최진영")
    private String author;

    @Schema(description = "책 표지 이미지 URL")
    private String coverImageUrl;

    @Schema(description = "책 전체 페이지 수 (마지막 페이지의 pageNumber)", example = "320")
    private Integer allPage;

    @Schema(description = "진행률(%). 완독 전에는 이번 첫 회독 기준, 완독한 뒤에는 지금 읽고 "
            + "있는 회차(book_status) 기준 진행률", example = "45.00")
    private BigDecimal progressRate;

    @Schema(description = "마지막으로 읽은 페이지 번호. 완독 전에는 이번 첫 회독, 완독한 뒤에는 "
            + "지금 회차(book_status) 기준", example = "42")
    private Integer lastPageNumber;

    @Schema(description = "마지막으로 읽은 페이지 ID. 완독 전에는 이번 첫 회독, 완독한 뒤에는 "
            + "지금 회차(book_status) 기준", example = "123")
    private Long lastPageId;

    @Schema(description = "마지막으로 읽은 시각 (아직 한 번도 안 읽었으면 null). 완독 전에는 "
            + "이번 첫 회독, 완독한 뒤에는 지금 회차(book_status) 기준")
    private LocalDateTime lastReadAt;

    @Schema(description = "완독 시각 (완독 전이면 null, 최초 완독 시각만 유지되고 재독해도 안 바뀜)")
    private LocalDateTime completedAt;

    @Schema(description = "이 책을 몇 번째 읽고 있는지(회독 수). 1이면 아직 첫 회독, "
            + "완독하면 다음 회차로 넘어가 값이 올라간다", example = "1")
    private Long repeatCount;
}

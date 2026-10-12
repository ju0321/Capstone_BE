package com.capstone.be.domain.reading_record.controller;

import com.capstone.be.domain.reading_record.dto.request.ReadingRecordCreateRequest;
import com.capstone.be.domain.reading_record.dto.request.ReadingRecordProgressUpdateRequest;
import com.capstone.be.domain.reading_record.dto.response.CurrentReadingResponse;
import com.capstone.be.domain.reading_record.dto.response.ReadingRecordListResponse;
import com.capstone.be.domain.reading_record.dto.response.ReadingRecordProgressResponse;
import com.capstone.be.domain.reading_record.dto.response.ReadingRecordResponse;
import com.capstone.be.domain.reading_record.enums.ReadingRecordStatus;
import com.capstone.be.domain.reading_record.service.ReadingRecordService;
import com.capstone.be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reading-records")
@RequiredArgsConstructor
@Validated
@Tag(name = "ReadingRecord", description = "독서 기록 - 도서 추가/완독 처리 API")
public class ReadingRecordController {

    private final ReadingRecordService readingRecordService;

    // 내 도서 목록에서 삭제하기
    @DeleteMapping("/{bookId}")
    @Operation(summary = "내 도서 목록에서 삭제하기",
            description = "로그인한 유저가 내 도서 목록에 추가한 책을 삭제합니다. 진행 기록/좋아요/북마크/하이라이트도 "
                    + "함께 삭제되며, 내 도서 목록에 없는 책이면 404(BR010) 응답")
    public ResponseEntity<BaseResponse<Void>> deleteBook(
            @RequestHeader("Authorization") String authorization, @PathVariable Long bookId) {
        readingRecordService.deleteBook(authorization, bookId);
        return ResponseEntity.ok(BaseResponse.success("내 도서 목록에서 삭제했습니다.", null));
    }

    // 홈 화면 - 최근 읽은 책
    @GetMapping("/current")
    @Operation(summary = "홈 화면 - 최근 읽은 책",
            description = "내 도서 목록(서랍) 기준으로 홈 화면에 보여줄 책 하나를 우선순위대로 고릅니다. "
                    + "1) 가장 최근에 읽은 책 2) 읽은 기록이 아예 없으면 읽다가 만 책 "
                    + "3) 그것도 없으면 data는 null")
    public ResponseEntity<BaseResponse<CurrentReadingResponse>> getCurrentBook(
            @RequestHeader("Authorization") String authorization) {
        CurrentReadingResponse response = readingRecordService.getCurrentBook(authorization);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 나의 도서 목록 보기
    @GetMapping
    @Operation(summary = "내 도서 목록 보기",
            description = "로그인한 유저가 내 도서 목록에 추가한 책을 무한스크롤(커서 기반)으로 조회합니다. "
                    + "status를 안 주거나 ALL이면 전체를, COMPLETED면 한 번이라도 완독한 책만 "
                    + "최근 등록순(recordId 내림차순)으로 반환합니다. "
                    + "cursor는 첫 요청에는 생략하고, 이후에는 이전 응답의 nextCursor를 그대로 보내면 "
                    + "이어서 다음 목록을 받습니다. hasNext가 false면 더 가져올 목록이 없다는 뜻입니다. "
                    + "항목별 repeatCount는 그 책을 몇 번째 읽고 있는지(회독 수)를 나타내고, "
                    + "progressRate/lastPageNumber/lastPageId/lastReadAt은 완독 전에는 첫 회독 "
                    + "기준, 완독한 뒤에는 지금 읽고 있는 회차(2회독 이상) 기준 값입니다.")
    public ResponseEntity<BaseResponse<ReadingRecordListResponse>> getMyBooks(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "ALL") ReadingRecordStatus status,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        ReadingRecordListResponse response = readingRecordService.getMyBooks(authorization, status, cursor, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 나의 도서 추가하기
    @PostMapping
    @Operation(summary = "내 도서 추가하기",
            description = "로그인한 유저가 책을 내 도서 목록에 추가함. 등록 시점 기준으로 "
                    + "마지막 페이지는 첫 페이지, 진행률은 0, 마지막 읽음/완료일은 비어있는 상태로 생성됨. "
                    + "이미 추가된 책이면 CONFLICT(BR009) 응답")
    public ResponseEntity<BaseResponse<ReadingRecordResponse>> addBook(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ReadingRecordCreateRequest request) {
        ReadingRecordResponse response = readingRecordService.addBook(authorization, request.getBookId());
        return ResponseEntity.ok(BaseResponse.success("책이 내 도서 목록에 추가되었습니다.", response));
    }

    // 진행률 갱신 (페이지 넘길 때)
    @PatchMapping("/{bookId}/progress")
    @Operation(summary = "페이지 넘길 때 진행률 갱신",
            description = "페이지를 넘길 때마다 호출해서 마지막 읽은 페이지/진행률/마지막으로 읽은 시각을 "
                    + "갱신합니다. lastReadAt은 매번 지금 시각으로 갱신되고, 마지막 페이지/진행률은 요청으로 "
                    + "들어온 페이지가 지금까지 도달한 페이지보다 뒤일 때만 갱신됩니다(뒤로 돌아가서 다시 봐도 "
                    + "진행률이 내려가지 않음). 진행률은 (현재 페이지 / 전체 페이지) * 100을 소수점 버림한 값입니다."
                    + "한 페이지에 3-4초 이상 머물러 있는 경우에만 해당 요청 진행(무지성하게 넘기는 문제 해결을 위해). "
                    + "이미 완독한 책이면 이 진행률은 회독 기록용으로만 쓰이고(1회독 완료 기록 자체는 안 바뀜), "
                    + "다시 마지막 페이지까지 도달하면 회독 수가 올라가고 처음부터 다시 추적됩니다.")
    public ResponseEntity<BaseResponse<ReadingRecordProgressResponse>> updateProgress(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long bookId,
            @Valid @RequestBody ReadingRecordProgressUpdateRequest request) {
        ReadingRecordProgressResponse response =
                readingRecordService.updateProgress(authorization, bookId, request.getPageId());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 책 완독 처리
    @PostMapping("/{bookId}/complete")
    @Operation(summary = "책 완독 처리",
            description = "로그인한 유저가 이 책을 완독했음을 기록합니다. 여러 번 호출해도 최초 완독 시각은 유지. "
                    + "호출할 때마다 회독 수가 1씩 올라가고 다음 회차 추적이 처음부터 다시 시작됨(페이지를 안 넘기고 "
                    + "바로 완독 처리하는 용도)")
    public ResponseEntity<BaseResponse<ReadingRecordResponse>> completeBook(
            @RequestHeader("Authorization") String authorization, @PathVariable Long bookId) {
        ReadingRecordResponse response = readingRecordService.completeBook(authorization, bookId);
        return ResponseEntity.ok(BaseResponse.success("완독 처리했습니다.", response));
    }
}

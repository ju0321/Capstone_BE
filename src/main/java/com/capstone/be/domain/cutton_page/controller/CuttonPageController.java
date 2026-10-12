package com.capstone.be.domain.cutton_page.controller;

import com.capstone.be.domain.cutton_page.dto.response.ChapterContentResponse;
import com.capstone.be.domain.cutton_page.dto.response.CuttonPageDetailResponse;
import com.capstone.be.domain.cutton_page.dto.response.PageStatusListResponse;
import com.capstone.be.domain.cutton_page.service.CuttonPageService;
import com.capstone.be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cutton-pages")
@RequiredArgsConstructor
@Validated
@Tag(name = "CuttonPage", description = "책 페이지 관련 api")
public class CuttonPageController {

    private final CuttonPageService cuttonPageService;

    // 책 페이지 내용 가져오기 (로그인 필수. 내 도서 목록에 추가한 책만 조회 가능)
    @GetMapping("/{pageId}")
    @Operation(summary = "책 페이지 내용 가져오기",
            description = "pageId로 해당 페이지의 문장/이미지 목록을 등장 순서대로 조회합니다. "
                    + "콘텐츠 자체는 Redis에 캐싱되어 DB 조회 없이 빠르게 응답합니다. "
                    + "로그인이 필요하며, 요청한 유저가 내 도서 목록에 추가하지 않은 책이면 "
                    + "(존재하지 않는 페이지와 동일하게) 404로 거절됩니다. "
                    + "정상 조회 시 그 유저의 좋아요/북마크/하이라이트 여부와, 이 페이지의 전체 "
                    + "좋아요 수/댓글 수(commentCount, 대댓글은 제외하고 최상위 댓글만 셉니다), "
                    + "이 책의 전체 페이지 수(allPage)도 같이 내려줍니다.")
    public ResponseEntity<BaseResponse<CuttonPageDetailResponse>> getPageContent(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long pageId) {
        CuttonPageDetailResponse response = cuttonPageService.getPageContentForUser(authorization, pageId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 진행 중인 챕터 내용 가져오기 (기준 페이지 앞/뒤 최대 10페이지씩, 로그인 필수/내 도서만 조회 가능)
    @GetMapping("/books/{bookId}/chapter/{pageNumber}")
    @Operation(summary = "진행 중인 페이지 앞뒤 내용 가져오기",
            description = "bookId와 그 책의 실제 페이지 번호(pageNumber)를 기준으로 앞으로 최대 "
                    + "10페이지, 뒤로 최대 10페이지를 함께 조회합니다. 책 시작/끝에 가까우면 그만큼만 "
                    + "내려갑니다. 이 책의 전체 페이지 수(allPage)도 같이 내려줍니다. "
                    + "pages의 각 항목에는 그 페이지의 댓글 수(commentCount, 대댓글은 제외하고 "
                    + "최상위 댓글만 셉니다)도 함께 내려갑니다. "
                    + "로그인이 필요하며, 요청한 유저가 내 도서 목록에 추가하지 않은 책이거나 그 "
                    + "책에 해당 페이지 번호가 없으면 (존재하지 않는 페이지와 동일하게) 404로 "
                    + "거절됩니다.")
    public ResponseEntity<BaseResponse<ChapterContentResponse>> getChapterContent(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long bookId,
            @PathVariable int pageNumber) {
        ChapterContentResponse response = cuttonPageService.getChapterContent(authorization, bookId, pageNumber);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 책 전체 내용 가져오기 (로그인 필수. 내 도서 목록에 추가한 책만 조회 가능)
    @GetMapping("/books/{bookId}")
    @Operation(summary = "책 전체 내용 가져오기",
            description = "bookId로 그 책의 모든 페이지를 페이지 번호 순으로 한 번에 조회합니다. "
                    + "이 책의 전체 페이지 수(allPage)도 같이 내려줍니다. 특정 챕터가 아니라 책 "
                    + "전체라 최상위 chapter 필드는 항상 null이며(각 페이지 안의 chapter는 그대로 "
                    + "들어있습니다), 나머지 필드 구조는 \"진행 중인 챕터 내용 가져오기\" 응답과 "
                    + "동일합니다(pages의 각 항목에 댓글 수 commentCount 포함, 대댓글 제외). "
                    + "로그인이 필요하며, 요청한 유저가 내 도서 목록에 추가하지 않은 "
                    + "책이면 (존재하지 않는 책과 동일하게) 404로 거절됩니다.")
    public ResponseEntity<BaseResponse<ChapterContentResponse>> getAllPagesForBook(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long bookId) {
        ChapterContentResponse response = cuttonPageService.getAllPagesForBook(authorization, bookId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 페이지 북마크하기
    @PostMapping("/{pageId}/bookmark")
    @Operation(summary = "페이지 북마크하기",
            description = "로그인한 유저가 해당 페이지를 북마크합니다. 이미 북마크한 상태라면 아무 변화 없이 "
                    + "그대로 성공 처리됩니다(멱등).")
    public ResponseEntity<BaseResponse<Void>> bookmarkPage(
            @RequestHeader("Authorization") String authorization, @PathVariable Long pageId) {
        cuttonPageService.addBookmark(authorization, pageId);
        return ResponseEntity.ok(BaseResponse.success("페이지를 북마크했습니다.", null));
    }

    // 페이지 북마크 취소
    @DeleteMapping("/{pageId}/bookmark")
    @Operation(summary = "페이지 북마크 취소",
            description = "로그인한 유저가 해당 페이지의 북마크를 취소합니다. 북마크하지 않은 상태여도 "
                    + "아무 변화 없이 그대로 성공 처리됩니다(멱등).")
    public ResponseEntity<BaseResponse<Void>> cancelBookmark(
            @RequestHeader("Authorization") String authorization, @PathVariable Long pageId) {
        cuttonPageService.removeBookmark(authorization, pageId);
        return ResponseEntity.ok(BaseResponse.success("페이지 북마크를 취소했습니다.", null));
    }

    // 페이지 좋아요 누르기
    @PostMapping("/{pageId}/like")
    @Operation(summary = "페이지 좋아요 누르기",
            description = "로그인한 유저가 해당 페이지에 좋아요를 누릅니다. 이미 좋아요한 상태라면 아무 변화 "
                    + "없이 그대로 성공 처리됩니다(멱등).")
    public ResponseEntity<BaseResponse<Void>> likePage(
            @RequestHeader("Authorization") String authorization, @PathVariable Long pageId) {
        cuttonPageService.addLike(authorization, pageId);
        return ResponseEntity.ok(BaseResponse.success("페이지에 좋아요를 눌렀습니다.", null));
    }

    // 페이지 좋아요 취소
    @DeleteMapping("/{pageId}/like")
    @Operation(summary = "페이지 좋아요 취소",
            description = "로그인한 유저가 해당 페이지의 좋아요를 취소합니다. 좋아요하지 않은 상태여도 "
                    + "아무 변화 없이 그대로 성공 처리됩니다(멱등).")
    public ResponseEntity<BaseResponse<Void>> cancelLike(
            @RequestHeader("Authorization") String authorization, @PathVariable Long pageId) {
        cuttonPageService.removeLike(authorization, pageId);
        return ResponseEntity.ok(BaseResponse.success("페이지 좋아요를 취소했습니다.", null));
    }

    // 나의 좋아요 목록 ("좋아요한 페이지" 화면의 "전체" 탭 - 무한스크롤)
    // (북마크 쪽의 동일한 목록 API는 domain/activity로 이전되었다 - ActivityController 참고.
    // 좋아요도 추후 activity 쪽으로 이전 예정이라 지금은 이 엔드포인트만 남아있다.)
    @GetMapping("/likes")
    @Operation(summary = "나의 좋아요 목록 (전체, 무한스크롤)",
            description = "로그인한 유저가 좋아요한 페이지를 가장 최근에 저장(좋아요)한 날짜순으로 조회합니다. "
                    + "각 항목은 책 이미지/제목, 실제 책 기준 페이지 번호, 그 페이지의 첫 문장 한 줄, "
                    + "저장한 날짜를 담습니다. 첫 요청은 cursorSavedAt/cursorId를 생략하고, 이후에는 "
                    + "이전 응답의 nextCursorSavedAt/nextCursorId를 그대로 함께 보내면 이어서 다음 목록을 "
                    + "받습니다(정렬 기준이 저장 날짜라 두 값을 같이 보내야 순서가 어긋나지 않습니다). "
                    + "좋아요를 취소했다가 다시 하면 그 시점 기준으로 목록 맨 위로 다시 올라옵니다.")
    public ResponseEntity<BaseResponse<PageStatusListResponse>> getLikedPages(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate cursorSavedAt,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        PageStatusListResponse response =
                cuttonPageService.getLikedPages(authorization, cursorSavedAt, cursorId, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 페이지 댓글 상태 보기
}

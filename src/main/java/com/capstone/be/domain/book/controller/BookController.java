package com.capstone.be.domain.book.controller;

import com.capstone.be.domain.book.dto.request.BookJsonCreateRequest;
import com.capstone.be.domain.book.dto.response.BookCreateResponse;
import com.capstone.be.domain.book.dto.response.BookDetailResponse;
import com.capstone.be.domain.book.dto.response.BookSearchResponse;
import com.capstone.be.domain.book.dto.response.GenreListItemResponse;
import com.capstone.be.domain.book.dto.response.PopularBooksResponse;
import com.capstone.be.domain.book.service.BookService;
import com.capstone.be.global.util.S3Uploader;
import com.capstone.be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Validated
@Tag(name = "Book", description = "책 관련 api")
public class BookController {

    private final BookService bookService;
    private final S3Uploader s3Uploader;

    // 책 정보 저장 api (json)
    @PostMapping(value = "/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "JSON형태 책 저장하기",
            description = "multipart/form-data로 전달합니다. 파트는 총 3개입니다. /n"
                    + "1) book (필수, Content-Type: application/json) - "
                    + "{\"title\":\"...\", \"author\":\"...\", \"publisher\":\"...\", "
                    + "\"description\":\"...\"(선택), \"genreId\":...(선택), "
                    + "\"createdAt\":\"yyyy-MM-dd\"} 형태의 JSON 문자열. genreId는 "
                    + "\"GET /api/v1/books/genres\"(관리자용)로 확인한 장르 ID를 넣으며, 비워두면 "
                    + "장르 없이 등록됩니다. 존재하지 않는 genreId를 보내면 404로 거절됩니다. /n"
                    + "2) coverImage (선택, 파일) - 책 표지 이미지. /n"
                    + "3) bookJsonFile (필수, 파일) - 파이썬(FastAPI) PDF 파싱 서비스가 만들어준 book_Json 결과 파일. /n"
                    + "주의: book 파트는 반드시 Content-Type을 application/json으로 지정해야 합니다 "
                    + "(그냥 텍스트로 보내면 파싱되지 않습니다).")
    public ResponseEntity<BaseResponse<BookCreateResponse>> createBookJson(
            @Valid @RequestPart("book") BookJsonCreateRequest book,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestPart("bookJsonFile") MultipartFile bookJsonFile) {
        // genreId 검증을 S3 업로드보다 먼저 한다 - 잘못된 genreId면 어차피 BookService에서
        // 저장이 실패하는데, 업로드부터 해버리면 실패한 책의 커버 이미지가 S3에 고아
        // 오브젝트로 남는다(DB 트랜잭션 롤백은 S3 업로드를 되돌리지 못함).
        bookService.validateGenreId(book.getGenreId());

        // S3 업로드는 트랜잭션(BookService.createBookJson) 시작 전에 끝내고, 완성된 URL만 넘긴다.
        // (S3 업로드는 DB 트랜잭션 롤백으로 되돌릴 수 없고, 업로드가 끝날 때까지 DB 커넥션을
        // 붙잡아두지 않기 위함)
        String coverImageUrl = (coverImage != null && !coverImage.isEmpty())
                ? s3Uploader.upload(coverImage)
                : null;
        BookCreateResponse response = bookService.createBookJson(book, coverImageUrl, bookJsonFile);
        return ResponseEntity.ok(BaseResponse.success("책 정보가 저장되었습니다.", response));
    }

    // 책 상세 보기
    @GetMapping("/{bookId}")
    @Operation(summary = "책 상세정보 보기",
            description = "bookId로 책의 상세 정보(제목/저자/표지/설명/장르/출판사/출판일)를 조회")
    public ResponseEntity<BaseResponse<BookDetailResponse>> getBookInfo(@PathVariable Long bookId) {
        BookDetailResponse response = bookService.getBookInfo(bookId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 책 검색 전 검색 페이지
    // TODO: 추천 도서 기준으로 뜨도록 수정 예정 (BookService.getPopularBooks() 참고)
    @GetMapping("/popular")
    @Operation(summary = "검색 전 책 인기순위",
            description = "top 100 인기도서를 20권씩 페이징하여 출력 /n"
            + "지금은 데이터 부족하므로 랜덤으로 가져오기 ")
    public ResponseEntity<BaseResponse<PopularBooksResponse>> popularBook(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        PopularBooksResponse response = bookService.getPopularBooks(page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 장르 전체 목록 보기 (관리자용 - 책 등록 시 넣을 genreId를 고르기 위한 참고용)
    @GetMapping("/genres")
    @Operation(summary = "장르 전체 목록 조회 (관리자용 API)",
            description = "관리자용 API입니다. genres 테이블에 고정으로 들어있는 장르 전체를 "
                    + "genreId/type/value 그대로 반환합니다. \"JSON형태 책 저장하기\" API로 책을 "
                    + "등록할 때 넣을 genreId를 고르는 용도입니다.")
    public ResponseEntity<BaseResponse<List<GenreListItemResponse>>> getAllGenres() {
        List<GenreListItemResponse> response = bookService.getAllGenres();
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // 책 검색하기 (제목/저자/출판사 통합 검색, 관련도순 상위 20권)
    @GetMapping("/search")
    @Operation(summary = "책 검색",
            description = "제목/저자/출판사 중 검색어가 포함된 책을 관련도순으로 최대 20권까지 반환 /n"
            + "사용자가 입력 시 200ms 정도 텀을 두고 실시간 요청하기 ")
    public ResponseEntity<BaseResponse<List<BookSearchResponse>>> searchBook(
            @RequestParam @NotBlank(message = "검색어를 입력해주세요.") String keyword) {
        List<BookSearchResponse> response = bookService.searchBooks(keyword);
        return ResponseEntity.ok(BaseResponse.success(response));
    }


}

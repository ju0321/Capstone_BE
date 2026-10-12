package com.capstone.be.domain.reading_record.service;

import com.capstone.be.domain.book.entity.Book;
import com.capstone.be.domain.book.entity.Page;
import com.capstone.be.domain.book.error.BookErrorCode;
import com.capstone.be.domain.book.repository.BookMaxPageNumberProjection;
import com.capstone.be.domain.book.repository.BookRepository;
import com.capstone.be.domain.book.repository.PageRepository;
import com.capstone.be.domain.book_status.entity.BookStatus;
import com.capstone.be.domain.book_status.repository.BookStatusRepository;
import com.capstone.be.domain.book_sentences.repository.HighlightRepository;
import com.capstone.be.domain.cutton_page.error.CuttonPageErrorCode;
import com.capstone.be.domain.cutton_page.repository.BookPageStatusRepository;
import com.capstone.be.domain.reading_record.dto.response.CurrentReadingResponse;
import com.capstone.be.domain.reading_record.dto.response.ReadingRecordListItemResponse;
import com.capstone.be.domain.reading_record.dto.response.ReadingRecordListResponse;
import com.capstone.be.domain.reading_record.dto.response.ReadingRecordProgressResponse;
import com.capstone.be.domain.reading_record.dto.response.ReadingRecordResponse;
import com.capstone.be.domain.reading_record.entity.ReadingRecord;
import com.capstone.be.domain.reading_record.enums.CurrentBookStatus;
import com.capstone.be.domain.reading_record.enums.ReadingRecordStatus;
import com.capstone.be.domain.reading_record.error.ReadingRecordErrorCode;
import com.capstone.be.domain.reading_record.repository.ReadingRecordRepository;
import com.capstone.be.global.exception.CustomException;
import com.capstone.be.global.exception.GlobalErrorCode;
import com.capstone.be.global.util.JwtUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ReadingRecordService {

    private final ReadingRecordRepository readingRecordRepository;
    private final BookRepository bookRepository;
    private final PageRepository pageRepository;
    private final BookPageStatusRepository bookPageStatusRepository;
    private final HighlightRepository highlightRepository;
    private final BookStatusRepository bookStatusRepository;
    private final JwtUtil jwtUtil;

    /** "완독" 판정 기준 진행률. progressRate가 이 값 이상이면 완독한 것으로 본다. */
    private static final BigDecimal COMPLETED_PROGRESS_RATE = BigDecimal.valueOf(100);

    @Transactional
    public ReadingRecordResponse addBook(String authorizationHeader, Long bookId) {
        Long userId = resolveUserId(authorizationHeader);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));

        if (readingRecordRepository.findByUserIdAndBookId(userId, bookId).isPresent()) {
            throw new CustomException(ReadingRecordErrorCode.READING_RECORD_ALREADY_EXISTS);
        }

        // 처음 등록하는 시점이라 "마지막으로 읽은 페이지"는 그 책의 첫 페이지로 채운다.
        // last_page_id는 cutton_pages를 참조하는 외래키라 0 같은 임의값은 넣을 수 없다.
        Page firstPage = pageRepository.findFirstByBook_BookIdOrderByPageNumberAsc(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));

        // 사전 조회(findByUserIdAndBookId)와 save() 사이에 동시에 같은 요청이 들어오면
        // uk_reading_records_user_book UNIQUE 제약 위반으로 save()가 실패할 수 있다 - 이 경우도
        // 사전 조회에서 걸린 것과 똑같이 READING_RECORD_ALREADY_EXISTS(BR009)로 응답해야
        // GlobalExceptionHandler의 일반 500으로 새지 않는다.
        ReadingRecord record;
        try {
            record = readingRecordRepository.save(
                    ReadingRecord.builder()
                            .userId(userId)
                            .bookId(bookId)
                            .lastPageId(firstPage.getPageId())
                            .progressRate(BigDecimal.ZERO)
                            .createdAt(LocalDateTime.now())
                            .lastReadAt(null)
                            .completedAt(null)
                            .build()
            );
            // 회독 기록용 book_status를 reading_record와 동시에 하나 만든다(1회독째, count=1,
            // 나머지 필드는 아직 안 읽었으니 비워둔다). 2회독부터는 이 행이 진행 상태를 대신 추적한다.
            bookStatusRepository.save(
                    BookStatus.builder()
                            .userId(userId)
                            .bookId(bookId)
                            .count(1L)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ReadingRecordErrorCode.READING_RECORD_ALREADY_EXISTS);
        }

        return ReadingRecordResponse.builder()
                .recordId(record.getRecordId())
                .bookId(book.getBookId())
                .bookTitle(book.getTitle())
                .progressRate(record.getProgressRate())
                .completedAt(record.getCompletedAt())
                .build();
    }

    /**
     * 내 도서 목록을 무한스크롤(커서 기반)로 조회한다. status=ALL(기본값)이면 전체,
     * status=COMPLETED면 완독한(progressRate가 100 이상인) 책만 최근 등록순(recordId 내림차순)으로
     * 반환한다.
     * <p>
     * cursor는 첫 요청이면 null, 그 뒤로는 이전 응답의 nextCursor를 그대로 보내면 이어서 다음
     * 목록을 받는다. hasNext를 별도 COUNT 쿼리 없이 판단하기 위해 리포지토리에서 size보다 1개
     * 더 가져온 뒤, 여기서 size개로 잘라내고 남은 1개가 있었는지로 hasNext를 정한다.
     * <p>
     * ReadingRecord는 bookId만 raw FK로 갖고 있어서(연관관계 없음), 책 제목/표지 이미지를
     * 같이 내려주기 위해 BookRepository.findAllById()로 한 번에 배치 조회해서 합친다(N+1 방지).
     */
    @Transactional(readOnly = true)
    public ReadingRecordListResponse getMyBooks(String authorizationHeader, ReadingRecordStatus status,
                                                 Long cursor, int size) {
        Long userId = resolveUserId(authorizationHeader);

        List<ReadingRecord> fetched = (status == ReadingRecordStatus.COMPLETED)
                ? readingRecordRepository.findByUserIdAndProgressRateGreaterThanEqualWithCursor(
                        userId, COMPLETED_PROGRESS_RATE, cursor, PageRequest.of(0, size + 1))
                : readingRecordRepository.findByUserIdWithCursor(userId, cursor, PageRequest.of(0, size + 1));

        boolean hasNext = fetched.size() > size;
        List<ReadingRecord> records = hasNext ? fetched.subList(0, size) : fetched;

        if (records.isEmpty()) {
            return ReadingRecordListResponse.builder()
                    .items(List.of())
                    .nextCursor(null)
                    .hasNext(false)
                    .build();
        }

        List<Long> bookIds = records.stream().map(ReadingRecord::getBookId).toList();
        Map<Long, Book> bookById = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getBookId, book -> book));

        // 책별 book_status(회독 수/현재 회차 진행 상태)도 같이 배치 조회한다(N+1 방지). 아직
        // book_status가 없는(레거시) 책은 아래에서 reading_record 자체 값으로 폴백한다.
        Map<Long, BookStatus> bookStatusByBookId = bookStatusRepository.findByUserIdAndBookIdIn(userId, bookIds)
                .stream()
                .collect(Collectors.toMap(BookStatus::getBookId, bookStatus -> bookStatus));

        // last_page_id가 가리키는 페이지 번호도 같이 내려주기 위해 배치 조회한다(N+1 방지).
        // 완독한 책은 아래에서 book_status.lastPageId(현재 회차 기준)를 대신 쓸 수 있으니
        // 그 페이지 ID들도 같이 모아서 한 번에 조회한다.
        List<Long> lastPageIds = Stream.concat(
                        records.stream().map(ReadingRecord::getLastPageId),
                        bookStatusByBookId.values().stream().map(BookStatus::getLastPageId))
                .filter(pageId -> pageId != null)
                .distinct()
                .toList();
        Map<Long, Integer> pageNumberByPageId = pageRepository.findAllById(lastPageIds).stream()
                .collect(Collectors.toMap(Page::getPageId, Page::getPageNumber));

        // 책 전체 페이지 수(allPage) = 그 책의 마지막 페이지 번호. updateProgress()의 진행률 계산과
        // 같은 기준이다. 목록에 나온 책들 것만 한 번에 배치 조회한다(N+1 방지).
        Map<Long, Integer> allPageByBookId = pageRepository.findMaxPageNumberByBookIds(bookIds).stream()
                .collect(Collectors.toMap(BookMaxPageNumberProjection::getBookId,
                        BookMaxPageNumberProjection::getMaxPageNumber));

        List<ReadingRecordListItemResponse> items = records.stream()
                .map(record -> {
                    Book book = bookById.get(record.getBookId());
                    BookStatus bookStatus = bookStatusByBookId.get(record.getBookId());

                    // 완독(completed_at IS NOT NULL) 이후에는 reading_record 자신의 progress_rate/
                    // last_page_id/last_read_at은 최초 완독 시점 값으로 영원히 고정되고(완독해도
                    // 안 바뀜), 그 뒤로는 book_status가 "지금 몇 회독째, 그 회차를 어디까지
                    // 읽었는지"를 대신 추적한다. 그래서 완독한 책은 book_status가 있고 그 안에
                    // lastPageId까지 채워져 있을 때(=완독 직후 정상적으로 다음 회차가 시작된
                    // 경우) book_status 값을 우선해서 보여준다. book_status가 아직 없는(이
                    // 기능이 생기기 전에 이미 완독된) 레거시 데이터는 reading_record 값으로
                    // 폴백한다.
                    boolean useBookStatus = record.getCompletedAt() != null
                            && bookStatus != null
                            && bookStatus.getLastPageId() != null;

                    BigDecimal progressRate = useBookStatus ? bookStatus.getProgressPercent() : record.getProgressRate();
                    Long lastPageId = useBookStatus ? bookStatus.getLastPageId() : record.getLastPageId();
                    LocalDateTime lastReadAt = useBookStatus ? bookStatus.getLastReadAt() : record.getLastReadAt();
                    Long repeatCount = bookStatus != null && bookStatus.getCount() != null ? bookStatus.getCount() : 1L;

                    return ReadingRecordListItemResponse.builder()
                            .recordId(record.getRecordId())
                            .bookId(record.getBookId())
                            .bookTitle(book != null ? book.getTitle() : null)
                            .author(book != null ? book.getAuthor() : null)
                            .coverImageUrl(book != null ? book.getCoverImageUrl() : null)
                            .allPage(allPageByBookId.get(record.getBookId()))
                            .progressRate(progressRate)
                            .lastPageNumber(pageNumberByPageId.get(lastPageId))
                            .lastPageId(lastPageId)
                            .lastReadAt(lastReadAt)
                            .completedAt(record.getCompletedAt())
                            .repeatCount(repeatCount)
                            .build();
                })
                .toList();

        Long nextCursor = hasNext ? records.get(records.size() - 1).getRecordId() : null;

        return ReadingRecordListResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    /**
     * 페이지를 넘길 때마다 호출해서 진행 상태를 갱신한다. bookId + 로그인한 유저 기준으로
     * 그 유저의 독서 기록을 찾는다(내 도서 목록에 없는 책이면 READING_RECORD_NOT_FOUND).
     * <p>
     * lastReadAt은 호출할 때마다 항상 지금 시각으로 갱신한다. 반면 lastPageId/progressRate는
     * 요청으로 들어온 페이지가 지금까지 도달한 페이지보다 "더 뒤"(pageNumber가 더 큼)일 때만
     * 갱신한다 - 그래야 이전 페이지로 돌아가서 다시 볼 때 진행률이 거꾸로 내려가지 않는다.
     * <p>
     * 진행률은 (지금 페이지 번호 / 책 전체 페이지 수) * 100을 소수점 버림(내림)해서 정수로 저장한다.
     * <p>
     * 이 책을 아직 한 번도 완독하지 않았으면(record.isCompleted()==false) 위 로직 그대로
     * reading_record 자체를 갱신한다(1회독). 이미 완독한 적이 있으면 reading_record는 최초
     * 완독 상태로 그대로 얼리고, 대신
     * book_status에서 지금 진행 중인 회차를 같은 방식(앞으로만 진행, 100%면 완료 후 다음
     * 회차로 리셋)으로 추적한다.
     */
    @Transactional
    public ReadingRecordProgressResponse updateProgress(String authorizationHeader, Long bookId, Long pageId) {
        Long userId = resolveUserId(authorizationHeader);

        // 동시 요청으로 인한 진행률 역행(lost update)을 막기 위해 비관적 락으로 조회한다.
        ReadingRecord record = readingRecordRepository.findByUserIdAndBookIdForUpdate(userId, bookId)
                .orElseThrow(() -> new CustomException(ReadingRecordErrorCode.READING_RECORD_NOT_FOUND));

        Page targetPage = pageRepository.findById(pageId)
                .orElseThrow(() -> new CustomException(CuttonPageErrorCode.PAGE_NOT_FOUND));
        if (!targetPage.getBook().getBookId().equals(record.getBookId())) {
            throw new CustomException(ReadingRecordErrorCode.PAGE_BOOK_MISMATCH);
        }

        LocalDateTime now = LocalDateTime.now();

        Integer resultLastPageNumber;
        BigDecimal resultProgress;
        LocalDateTime resultLastReadAt;

        if (!record.isCompleted()) {
            Page currentLastPage = pageRepository.findById(record.getLastPageId()).orElse(null);
            boolean isForwardProgress = currentLastPage == null
                    || targetPage.getPageNumber() > currentLastPage.getPageNumber();

            if (isForwardProgress) {
                Page finalPage = pageRepository.findFirstByBook_BookIdOrderByPageNumberDesc(record.getBookId())
                        .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
                BigDecimal progressRate = calcProgressPercent(targetPage.getPageNumber(), finalPage.getPageNumber());
                record.advanceProgress(targetPage.getPageId(), progressRate, now);
                if (progressRate.compareTo(COMPLETED_PROGRESS_RATE) == 0) {
                    record.completeIfNotAlready(now);
                    // 1회독이 지금 막 끝났다 - 회독 기록을 2회독째로 넘기고 처음부터 다시 추적한다.
                    forceCompleteCurrentPass(userId, bookId, now);
                }
            } else {
                record.touchLastReadAt(now);
            }

            Page lastPage = pageRepository.findById(record.getLastPageId()).orElse(targetPage);
            resultLastPageNumber = lastPage.getPageNumber();
            resultProgress = record.getProgressRate();
            resultLastReadAt = record.getLastReadAt();
        } else {
            BookStatus status = bookStatusRepository.findByUserIdAndBookIdForUpdate(userId, bookId)
                    .orElseGet(() -> bookStatusRepository.save(
                            BookStatus.builder().userId(userId).bookId(bookId).count(1L).build()));

            Page statusLastPage = status.getLastPageId() != null
                    ? pageRepository.findById(status.getLastPageId()).orElse(null)
                    : null;
            boolean isForwardProgress = statusLastPage == null
                    || targetPage.getPageNumber() > statusLastPage.getPageNumber();

            if (isForwardProgress) {
                Page finalPage = pageRepository.findFirstByBook_BookIdOrderByPageNumberDesc(record.getBookId())
                        .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
                BigDecimal progress = calcProgressPercent(targetPage.getPageNumber(), finalPage.getPageNumber());
                status.advance(targetPage.getPageId(), progress, now);
                if (progress.compareTo(COMPLETED_PROGRESS_RATE) == 0) {
                    // 이번 회차도 끝났다 - 곧바로 다음 회차로 넘기고 처음부터 다시 추적한다.
                    Long firstPageId = pageRepository.findFirstByBook_BookIdOrderByPageNumberAsc(record.getBookId())
                            .map(Page::getPageId)
                            .orElse(null);
                    status.completeCurrentPassAndStartNext(firstPageId, now);
                }
            } else {
                status.touchLastReadAt(now);
            }

            Page statusPage = status.getLastPageId() != null
                    ? pageRepository.findById(status.getLastPageId()).orElse(targetPage)
                    : targetPage;
            resultLastPageNumber = statusPage.getPageNumber();
            resultProgress = status.getProgressPercent();
            resultLastReadAt = status.getLastReadAt();
        }

        return ReadingRecordProgressResponse.builder()
                .recordId(record.getRecordId())
                .lastPageNumber(resultLastPageNumber)
                .progressRate(resultProgress)
                .lastReadAt(resultLastReadAt)
                .build();
    }

    /** (페이지 번호 / 책 전체 페이지 수) * 100을 소수점 버림(내림)해서 정수로, 100을 넘지 않게 계산한다. */
    private BigDecimal calcProgressPercent(int pageNumber, int finalPageNumber) {
        return BigDecimal.valueOf(pageNumber)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(finalPageNumber), 0, RoundingMode.DOWN)
                .min(BigDecimal.valueOf(100));
    }

    /**
     * 지금 진행 중인 회차(book_status)를 그 자리에서 강제로 완료 처리하고, 곧바로 다음 회차를
     * 처음(첫 페이지/0%)부터 추적하기 시작한다. updateProgress()가 페이지 진행으로 100%를
     * 찍었을 때와, completeBook()으로 바로 완독 처리할 때 둘 다 여기서 처리한다.
     * book_status가 아직 없으면(addBook() 없이 completeBook()부터 부른 경우 등 레거시 상황)
     * 1회독으로 새로 만든 뒤 곧바로 2회독으로 넘긴다.
     */
    private void forceCompleteCurrentPass(Long userId, Long bookId, LocalDateTime now) {
        Long firstPageId = pageRepository.findFirstByBook_BookIdOrderByPageNumberAsc(bookId)
                .map(Page::getPageId)
                .orElse(null);

        BookStatus status = bookStatusRepository.findByUserIdAndBookIdForUpdate(userId, bookId)
                .orElseGet(() -> bookStatusRepository.save(
                        BookStatus.builder().userId(userId).bookId(bookId).count(1L).build()));

        status.completeCurrentPassAndStartNext(firstPageId, now);
    }

    /**
     * 내 도서 목록에서 책을 삭제한다.
     * <p>
     * 페이지 좋아요/북마크
     * (book_page_status)와 문장 하이라이트(highlights)는 user_id/page_id, user_id/sentence_id로만
     * 연결돼 있고 reading_records를 참조하지 않아서, 여기서 이 책 범위만큼 직접 같이 지워줘야
     * 고아 데이터가 안 남는다.
     */
    @Transactional
    public void deleteBook(String authorizationHeader, Long bookId) {
        Long userId = resolveUserId(authorizationHeader);

        readingRecordRepository.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new CustomException(ReadingRecordErrorCode.READING_RECORD_NOT_FOUND));

        bookPageStatusRepository.deleteByUserIdAndBookId(userId, bookId);
        highlightRepository.deleteByUserIdAndBookId(userId, bookId);
        bookStatusRepository.deleteByUserIdAndBookId(userId, bookId);
        readingRecordRepository.deleteByUserIdAndBookId(userId, bookId);
    }

    /**
     * 홈 화면 "최근 읽은 책" 카드를 위한 책 하나를 우선순위대로 고른다. 전부 내 도서 목록
     * (reading_records)에 있는 책 기준이다.
     * <p>
     * 1) lastReadAt이 있는 기록 중 가장 최근 것.
     * 2) lastReadAt이 하나도 없으면 - 읽다가 만(진행률 0~100% 사이) 책 중 가장 최근 등록된 것.
     * 3) 그것도 없으면 - null(보여줄 책 없음).
     */
    @Transactional(readOnly = true)
    public CurrentReadingResponse getCurrentBook(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);

        Optional<ReadingRecord> mostRecentlyRead =
                readingRecordRepository.findFirstByUserIdAndLastReadAtIsNotNullOrderByLastReadAtDesc(userId);
        if (mostRecentlyRead.isPresent()) {
            return buildReading(mostRecentlyRead.get());
        }

        List<ReadingRecord> inProgress = readingRecordRepository.findInProgress(userId);
        if (!inProgress.isEmpty()) {
            return buildReading(inProgress.get(0));
        }

        return null;
    }

    private CurrentReadingResponse buildReading(ReadingRecord record) {
        Book book = bookRepository.findById(record.getBookId())
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));

        Integer remainingPages = null;
        Page lastReadPage = pageRepository.findById(record.getLastPageId()).orElse(null);
        Page finalPage = pageRepository.findFirstByBook_BookIdOrderByPageNumberDesc(record.getBookId()).orElse(null);
        if (lastReadPage != null && finalPage != null) {
            remainingPages = Math.max(finalPage.getPageNumber() - lastReadPage.getPageNumber(), 0);
        }

        return CurrentReadingResponse.builder()
                .status(CurrentBookStatus.READING)
                .recordId(record.getRecordId())
                .bookId(book.getBookId())
                .bookTitle(book.getTitle())
                .coverImageUrl(book.getCoverImageUrl())
                .progressRate(record.getProgressRate())
                .remainingPages(remainingPages)
                .build();
    }

    /**
     * 페이지를 하나씩 넘기지 않고 바로 "완독 처리"하는 API. reading_record의 completed_at은
     * 최초 1번만 세팅되고 그 뒤로는 안 바뀌지만(upsertCompleted()의 COALESCE), 회독
     * 기록(book_status)은 이 버튼을 누를 때마다 "지금 회차가 끝났다"는 뜻으로 매번 다음
     * 회차로 넘긴다 - updateProgress()로 페이지를 넘기다 100%를 찍었을 때와 동일하게 취급한다.
     */
    @Transactional
    public ReadingRecordResponse completeBook(String authorizationHeader, Long bookId) {
        Long userId = resolveUserId(authorizationHeader);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));
        Page lastPage = pageRepository.findFirstByBook_BookIdOrderByPageNumberDesc(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));

        readingRecordRepository.upsertCompleted(userId, bookId, lastPage.getPageId(), LocalDateTime.now());

        ReadingRecord record = readingRecordRepository.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new CustomException(ReadingRecordErrorCode.READING_RECORD_NOT_FOUND));

        forceCompleteCurrentPass(userId, bookId, LocalDateTime.now());

        return ReadingRecordResponse.builder()
                .recordId(record.getRecordId())
                .bookId(book.getBookId())
                .bookTitle(book.getTitle())
                .progressRate(record.getProgressRate())
                .completedAt(record.getCompletedAt())
                .build();
    }

    private Long resolveUserId(String authorizationHeader) {
        return jwtUtil.getUserIdFromToken(extractAccessToken(authorizationHeader));
    }

    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }
        return authorizationHeader.substring("Bearer ".length());
    }
}

package com.capstone.be.domain.cutton_page.service;

import com.capstone.be.domain.book.entity.Book;
import com.capstone.be.domain.book.entity.Page;
import com.capstone.be.domain.book.repository.BookRepository;
import com.capstone.be.domain.book.repository.FirstSentenceProjection;
import com.capstone.be.domain.book.repository.PageRepository;
import com.capstone.be.domain.book.repository.SentenceRepository;
import com.capstone.be.domain.book_sentences.entity.SentenceHighlight;
import com.capstone.be.domain.book_sentences.enums.HighlightColor;
import com.capstone.be.domain.book_sentences.repository.HighlightRepository;
import com.capstone.be.domain.reading_record.repository.ReadingRecordRepository;
import com.capstone.be.domain.cutton_page.dto.response.ChapterContentResponse;
import com.capstone.be.domain.cutton_page.dto.response.CuttonPageDetailResponse;
import com.capstone.be.domain.cutton_page.dto.response.CuttonPageListItemResponse;
import com.capstone.be.domain.cutton_page.dto.response.CuttonPageResponse;
import com.capstone.be.domain.cutton_page.dto.response.PageStatusListItemResponse;
import com.capstone.be.domain.cutton_page.dto.response.PageStatusListResponse;
import com.capstone.be.domain.cutton_page.dto.response.SentenceResponse;
import com.capstone.be.domain.cutton_page.entity.BookPageStatus;
import com.capstone.be.domain.cutton_page.enums.PageReactionType;
import com.capstone.be.domain.cutton_page.error.CuttonPageErrorCode;
import com.capstone.be.domain.cutton_page.repository.BookPageStatusRepository;
import com.capstone.be.domain.page_comment.repository.PageCommentCountProjection;
import com.capstone.be.domain.page_comment.repository.PageCommentRepository;
import com.capstone.be.domain.user.repository.UserRepository;
import com.capstone.be.global.exception.CustomException;
import com.capstone.be.global.exception.GlobalErrorCode;
import com.capstone.be.global.util.JwtUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CuttonPageService {

    private final CuttonPageContentService cuttonPageContentService;
    private final PageRepository pageRepository;
    private final BookRepository bookRepository;
    private final SentenceRepository sentenceRepository;
    private final BookPageStatusRepository bookPageStatusRepository;
    private final PageCommentRepository pageCommentRepository;
    private final UserRepository userRepository;
    private final HighlightRepository highlightRepository;
    private final ReadingRecordRepository readingRecordRepository;
    private final JwtUtil jwtUtil;

    /** "진행 중인 챕터 내용 가져오기"에서 기준 페이지 앞/뒤로 각각 가져올 최대 페이지 수. */
    private static final int CHAPTER_PAGE_WINDOW = 10;

    /**
     * pageId로 책 페이지 내용만 조회한다(좋아요/북마크 상태 없이). 콘텐츠 캐싱은
     * CuttonPageContentService.getPageContent() 참고.
     */
    public CuttonPageResponse getPageContent(Long pageId) {
        return cuttonPageContentService.getPageContent(pageId);
    }

    /**
     * pageId로 책 페이지 내용을 조회하면서, 요청한 유저의 좋아요/북마크/하이라이트 상태를 같이
     * 얹어서 반환한다. 로그인이 필수이며, 요청한 유저가 해당 책을 내 도서 목록(reading_records)에
     * 추가해둔 적이 없으면 PAGE_NOT_FOUND(404)로 거절한다 - 단, 테스트 우회 유저(userId 1~4)는
     * 이 소유 여부 체크를 건너뛰고 모든 책을 조회할 수 있다.
     * <p>
     * 소유하지 않은 책이라는 걸 403 등으로 따로 알려주면 pageId를 대입해보는 것만으로 "이 페이지가
     * 실제로 존재한다"는 정보가 새어나갈 수 있어서(IDOR/정보 노출), 존재하지 않는 페이지와 똑같이
     * PAGE_NOT_FOUND(404)로 응답한다.
     * <p>
     * 콘텐츠(문장/이미지 목록)는 CuttonPageContentService에서 pageId 기준으로 캐싱된 걸 그대로
     * 가져오고 - 이 콘텐츠는 모든 유저가 공유한다. 좋아요/북마크 여부는 유저마다 다른 값이라
     * 캐싱하지 않고, BookPageStatusRepository.findByUserIdAndPageId()로 매 요청마다 가볍게
     * (인덱스 걸린 단순 조회) 확인해서 응답에 얹는다. 그래서 이 메서드 자체는 캐시 대상이 아니다.
     */
    @Transactional(readOnly = true)
    public CuttonPageDetailResponse getPageContentForUser(String authorizationHeader, Long pageId) {
        Long userId = resolveUserId(authorizationHeader);

        CuttonPageResponse content = cuttonPageContentService.getPageContent(pageId);

        if (!jwtUtil.isTestBypassUser(userId)
                && readingRecordRepository.findByUserIdAndBookId(userId, content.getBookId()).isEmpty()) {
            throw new CustomException(CuttonPageErrorCode.PAGE_NOT_FOUND);
        }

        // book_page_status는 이제 유저+페이지당 반응(북마크/좋아요) 행이 0~2개(타입별로 최대
        // 1개씩) 나오는 구조라, 예전처럼 row 하나에서 boolean 두 개를 꺼내는 게 아니라 타입
        // 목록에 BOOKMARK/LIKE가 있는지로 판단한다.
        List<PageReactionType> types = bookPageStatusRepository.findByUserIdAndPageId(userId, pageId).stream()
                .map(BookPageStatus::getType)
                .toList();
        boolean bookmarked = types.contains(PageReactionType.BOOKMARK);
        boolean liked = types.contains(PageReactionType.LIKE);

        // 좋아요/댓글 수는 이 유저만이 아니라 전체 유저 기준 집계값이라 bookmarked/liked와
        // 별개로 매 요청마다 COUNT 쿼리로 조회한다.
        long likeCount = bookPageStatusRepository.countByPageIdAndType(pageId, PageReactionType.LIKE);
        long commentCount = pageCommentRepository.countByPage_PageIdAndParentCommentIsNull(pageId);

        // 책 전체 페이지 수 = 그 책의 마지막 페이지 번호 (ReadingRecordService의 진행률 계산과 동일한 기준).
        Integer allPage = pageRepository.findFirstByBook_BookIdOrderByPageNumberDesc(content.getBookId())
                .map(Page::getPageNumber)
                .orElse(null);

        return CuttonPageDetailResponse.builder()
                .pageId(content.getPageId())
                .bookId(content.getBookId())
                .pageNumber(content.getPageNumber())
                .allPage(allPage)
                .chapter(content.getChapter())
                .imageUrl(content.getImageUrl())
                .sentences(attachHighlightColors(content.getSentences(), userId))
                .bookmarked(bookmarked)
                .liked(liked)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .build();
    }

    /**
     * 캐싱된 공용 콘텐츠(content.getSentences())는 유저별 하이라이트 색을 담고 있지 않으므로,
     * 이 유저가 이 페이지 안에서 수집(하이라이트)한 문장이 있으면 그 색을 얹어서 새 목록을
     * 만들어 반환한다. 비로그인(userId=null)이거나 하이라이트가 하나도 없으면 원본 목록을
     * 그대로 돌려준다(불필요한 재조립/쿼리를 피함).
     */
    private List<SentenceResponse> attachHighlightColors(List<SentenceResponse> sentences, Long userId) {
        if (userId == null || sentences.isEmpty()) {
            return sentences;
        }

        List<Long> sentenceIds = sentences.stream().map(SentenceResponse::getSentenceId).toList();
        Map<Long, HighlightColor> colorBySentenceId = highlightRepository
                .findByUserIdAndSentenceIdIn(userId, sentenceIds).stream()
                .collect(Collectors.toMap(SentenceHighlight::getSentenceId, SentenceHighlight::getColor));

        if (colorBySentenceId.isEmpty()) {
            return sentences;
        }

        return sentences.stream()
                .map(sentence -> SentenceResponse.builder()
                        .sentenceId(sentence.getSentenceId())
                        .sentenceIndex(sentence.getSentenceIndex())
                        .content(sentence.getContent())
                        .status(sentence.getStatus())
                        .highlightColor(colorBySentenceId.get(sentence.getSentenceId()))
                        .build())
                .toList();
    }

    /**
     * bookId + 실제 페이지 번호(pageNumber) 기준으로 앞뒤 최대 CHAPTER_PAGE_WINDOW(10)페이지씩,
     * 그 책 안에서 실제로 존재하는 페이지만 pageNumber 순으로 조회한다. 책 시작/끝에 가까워서
     * 10페이지가 안 남았으면 있는 만큼만 내려간다(예: 기준 페이지가 3번이면 앞으로는 1~2번 두
     * 페이지만 내려감).
     * <p>
     * 예전에는 pageId(페이지 PK)를 입력받았지만, 클라이언트가 책의 실제 페이지 번호만 알고
     * 있어도 바로 호출할 수 있도록 bookId+pageNumber를 입력받게 바꿨다.
     * <p>
     * getPageContentForUser()와 동일하게 로그인이 필수이고, 요청한 유저가 내 도서 목록에
     * 추가하지 않은 책이면 거절한다(테스트 우회 유저는 예외) - bookId+pageNumber만 알면 소유
     * 여부 체크 없이 주변 최대 21페이지 내용을 통째로 가져갈 수 있었던 구멍(IDOR)을 막기
     * 위함이다. 존재하지 않는 페이지와 구분되지 않도록 PAGE_NOT_FOUND(404)로 동일하게
     * 응답한다(책 자체가 없거나 그 책에 해당 pageNumber가 없는 경우도 마찬가지다).
     * <p>
     * 챕터 경계를 기준으로 자르던 기존 방식은 챕터 데이터가 불확실한 경우가 있어 페이지 번호
     * 기준의 고정 윈도우 방식으로 바꿨다. chapter 필드는 기준 페이지가 속한 챕터명을 그대로
     * 참고용으로 내려준다.
     * <p>
     * 페이지 내용 자체는 CuttonPageContentService.getPageContent()를 그대로 재사용해서
     * Redis 캐시 이득을 그대로 받는다(자기 자신을 호출하는 게 아니라 다른 빈을 통해 호출하므로
     * @Cacheable 프록시가 정상 동작한다).
     */
    @Transactional(readOnly = true)
    public ChapterContentResponse getChapterContent(String authorizationHeader, Long bookId, int pageNumber) {
        Long userId = resolveUserId(authorizationHeader);

        Page currentPage = pageRepository.findByBook_BookIdAndPageNumber(bookId, pageNumber)
                .orElseThrow(() -> new CustomException(CuttonPageErrorCode.PAGE_NOT_FOUND));

        if (!jwtUtil.isTestBypassUser(userId)
                && readingRecordRepository.findByUserIdAndBookId(userId, bookId).isEmpty()) {
            throw new CustomException(CuttonPageErrorCode.PAGE_NOT_FOUND);
        }

        int startPageNumber = Math.max(pageNumber - CHAPTER_PAGE_WINDOW, 1);
        int endPageNumber = pageNumber + CHAPTER_PAGE_WINDOW;

        List<Page> surroundingPages = pageRepository.findByBook_BookIdAndPageNumberBetweenOrderByPageNumberAsc(
                bookId, startPageNumber, endPageNumber);

        List<CuttonPageListItemResponse> pages = toListItems(surroundingPages);

        // 책 전체 페이지 수 = 그 책의 마지막 페이지 번호 (getPageContentForUser()와 동일한 기준).
        Integer allPage = pageRepository.findFirstByBook_BookIdOrderByPageNumberDesc(bookId)
                .map(Page::getPageNumber)
                .orElse(null);

        return ChapterContentResponse.builder()
                .chapter(currentPage.getChapter())
                .allPage(allPage)
                .pages(pages)
                .build();
    }

    /**
     * bookId로 책 전체 내용을 페이지 번호 순으로 한 번에 조회한다. getChapterContent()와 동일한
     * ChapterContentResponse(chapter/allPage/pages) 형태를 그대로 재사용하지만, 특정 챕터가
     * 아니라 책 전체라 최상위 chapter 필드는 의미가 없어 null로 내려준다(각 페이지 안의
     * chapter 필드는 그대로 유지된다).
     * <p>
     * getPageContentForUser()/getChapterContent()와 동일하게 로그인이 필수이며, 요청한
     * 유저가 내 도서 목록(reading_records)에 추가하지 않은 책이면 (존재하지 않는 책과
     * 동일하게) BOOK_NOT_FOUND(404)로 거절한다(테스트 우회 유저는 예외) - IDOR 방지를 위해
     * 소유 여부와 무관하게 항상 같은 응답으로 통일한다.
     * <p>
     * 페이지 내용 자체는 CuttonPageContentService.getPageContent()를 페이지마다 그대로
     * 재사용해서 Redis 캐시 이득을 그대로 받는다.
     */
    @Transactional(readOnly = true)
    public ChapterContentResponse getAllPagesForBook(String authorizationHeader, Long bookId) {
        Long userId = resolveUserId(authorizationHeader);

        if (!bookRepository.existsById(bookId)) {
            throw new CustomException(CuttonPageErrorCode.BOOK_NOT_FOUND);
        }
        if (!jwtUtil.isTestBypassUser(userId)
                && readingRecordRepository.findByUserIdAndBookId(userId, bookId).isEmpty()) {
            throw new CustomException(CuttonPageErrorCode.BOOK_NOT_FOUND);
        }

        List<Page> allPages = pageRepository.findByBook_BookIdOrderByPageNumberAsc(bookId);

        List<CuttonPageListItemResponse> pages = toListItems(allPages);

        Integer allPage = allPages.isEmpty() ? null : allPages.get(allPages.size() - 1).getPageNumber();

        return ChapterContentResponse.builder()
                .chapter(null)
                .allPage(allPage)
                .pages(pages)
                .build();
    }

    /**
     * getChapterContent()/getAllPagesForBook() 공용 - 페이지 목록을 캐싱된 콘텐츠 +
     * 댓글 수(최상위 댓글만, 대댓글 제외)로 조립한다. 댓글 수는 페이지마다 따로 조회하면
     * N+1이 나므로 PageCommentRepository.countRootCommentsByPageIds()로 한 번에 배치
     * 조회한다. 댓글이 하나도 없는 페이지는 그 쿼리 결과에 아예 안 나오므로 0으로 기본
     * 처리한다.
     */
    private List<CuttonPageListItemResponse> toListItems(List<Page> pages) {
        if (pages.isEmpty()) {
            return List.of();
        }

        List<Long> pageIds = pages.stream().map(Page::getPageId).toList();
        Map<Long, Long> commentCountByPageId = pageCommentRepository.countRootCommentsByPageIds(pageIds).stream()
                .collect(Collectors.toMap(
                        PageCommentCountProjection::getPageId, PageCommentCountProjection::getCommentCount));

        return pages.stream()
                .map(page -> {
                    CuttonPageResponse content = cuttonPageContentService.getPageContent(page.getPageId());
                    return CuttonPageListItemResponse.builder()
                            .pageId(content.getPageId())
                            .bookId(content.getBookId())
                            .pageNumber(content.getPageNumber())
                            .chapter(content.getChapter())
                            .imageUrl(content.getImageUrl())
                            .sentences(content.getSentences())
                            .commentCount(commentCountByPageId.getOrDefault(page.getPageId(), 0L))
                            .build();
                })
                .toList();
    }

    /**
     * 페이지를 북마크한다. 이미 북마크되어 있으면 아무 일도 하지 않는다(멱등) - 유니크 제약
     * (page_id, user_id, type) 덕분에 INSERT ... ON CONFLICT DO NOTHING 하나로 동시 요청도
     * 안전하게 처리된다.
     */
    @Transactional
    public void addBookmark(String authorizationHeader, Long pageId) {
        Long userId = resolveUserId(authorizationHeader);
        validatePageOwnedByUser(userId, pageId);
        bookPageStatusRepository.insertReaction(userId, pageId, PageReactionType.BOOKMARK.name());
    }

    /**
     * 페이지 북마크를 취소한다(반응 row 자체를 삭제). 북마크가 없었어도 조용히 끝난다
     * (멱등한 취소, CommentReactionRepository의 좋아요/싫어요 취소와 동일한 패턴).
     */
    @Transactional
    public void removeBookmark(String authorizationHeader, Long pageId) {
        Long userId = resolveUserId(authorizationHeader);
        validatePageAndUser(userId, pageId);
        bookPageStatusRepository.deleteByUserIdAndPageIdAndType(userId, pageId, PageReactionType.BOOKMARK);
    }

    /** addBookmark()와 대칭 - 좋아요 버전. */
    @Transactional
    public void addLike(String authorizationHeader, Long pageId) {
        Long userId = resolveUserId(authorizationHeader);
        validatePageOwnedByUser(userId, pageId);
        bookPageStatusRepository.insertReaction(userId, pageId, PageReactionType.LIKE.name());
    }

    /** removeBookmark()와 대칭 - 좋아요 취소 버전. */
    @Transactional
    public void removeLike(String authorizationHeader, Long pageId) {
        Long userId = resolveUserId(authorizationHeader);
        validatePageAndUser(userId, pageId);
        bookPageStatusRepository.deleteByUserIdAndPageIdAndType(userId, pageId, PageReactionType.LIKE);
    }

    /**
     * 나의 좋아요 목록(전체 탭) - 가장 최근에 좋아요한 순으로 무한스크롤 조회한다. 북마크 쪽의
     * 동일한 목록 조회는 domain/activity의 ActivityService로 이전되었다(좋아요도 추후 이전 예정).
     */
    @Transactional(readOnly = true)
    public PageStatusListResponse getLikedPages(String authorizationHeader, LocalDate cursorSavedAt,
                                                 Long cursorId, int size) {
        Long userId = resolveUserId(authorizationHeader);
        validateCursorPair(cursorSavedAt, cursorId);
        return getPageStatusList(userId, PageReactionType.LIKE, cursorSavedAt, cursorId, size);
    }

    /**
     * getLikedPages 내부 로직. type 파라미터는 현재 LIKE 고정으로만 호출되지만, 북마크 쪽
     * 로직과 완전히 동일해서(커서 처리, 책/페이지/첫 문장 배치 조회, 응답 조립) 그대로 남겨뒀다.
     * <p>
     * hasNext를 별도 COUNT 쿼리 없이 판단하기 위해 리포지토리에서 size보다 1개 더 가져온 뒤,
     * 여기서 size개로 잘라내고 남은 1개가 있었는지로 hasNext를 정한다(다른 커서 목록 API들과
     * 동일한 패턴).
     */
    private PageStatusListResponse getPageStatusList(Long userId, PageReactionType type,
                                                       LocalDate cursorSavedAt, Long cursorId, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);
        List<BookPageStatus> fetched = bookPageStatusRepository
                .findByUserIdAndTypeWithCursor(userId, type, cursorSavedAt, cursorId, pageable);

        boolean hasNext = fetched.size() > size;
        List<BookPageStatus> statuses = hasNext ? fetched.subList(0, size) : fetched;

        if (statuses.isEmpty()) {
            return PageStatusListResponse.builder()
                    .items(List.of())
                    .nextCursorSavedAt(null)
                    .nextCursorId(null)
                    .hasNext(false)
                    .build();
        }

        // 책 제목/표지 이미지를 같이 내려주기 위해 Page -> Book 순으로 배치 조회한다(N+1 방지).
        // Page.book은 LAZY 연관관계지만 getBookId()는 FK 컬럼이라 프록시 초기화(추가 쿼리) 없이 읽힌다.
        List<Long> pageIds = statuses.stream().map(BookPageStatus::getPageId).toList();
        Map<Long, Page> pageById = pageRepository.findAllById(pageIds).stream()
                .collect(Collectors.toMap(Page::getPageId, page -> page));

        List<Long> bookIds = pageById.values().stream()
                .map(page -> page.getBook().getBookId())
                .distinct()
                .toList();
        Map<Long, Book> bookById = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getBookId, book -> book));

        Map<Long, String> firstSentenceByPageId = sentenceRepository
                .findFirstSentenceContentByPageIds(pageIds).stream()
                .collect(Collectors.toMap(FirstSentenceProjection::getPageId, FirstSentenceProjection::getContent));

        List<PageStatusListItemResponse> items = statuses.stream()
                .map(status -> {
                    Page page = pageById.get(status.getPageId());
                    Book book = page != null ? bookById.get(page.getBook().getBookId()) : null;
                    return PageStatusListItemResponse.builder()
                            .pageId(status.getPageId())
                            .bookId(book != null ? book.getBookId() : null)
                            .bookTitle(book != null ? book.getTitle() : null)
                            .coverImageUrl(book != null ? book.getCoverImageUrl() : null)
                            .pageNumber(page != null ? page.getPageNumber() : null)
                            .firstSentence(firstSentenceByPageId.get(status.getPageId()))
                            .savedAt(status.getCreatedAt())
                            .build();
                })
                .toList();

        BookPageStatus last = statuses.get(statuses.size() - 1);
        LocalDate nextCursorSavedAt = hasNext ? last.getCreatedAt() : null;
        Long nextCursorId = hasNext ? last.getRecordPageId() : null;

        return PageStatusListResponse.builder()
                .items(items)
                .nextCursorSavedAt(nextCursorSavedAt)
                .nextCursorId(nextCursorId)
                .hasNext(hasNext)
                .build();
    }

    /**
     * 페이지/유저가 실제로 존재하는지 확인한다. 탈퇴한 유저의 아직 만료 전인 토큰으로 요청이
     * 와도 여기서 걸러진다(RESOURCE_NOT_FOUND) - UserService.withdraw()가 탈퇴 시 토큰을
     * 즉시 블랙리스트에 올리므로 대부분은 여기까지 오지도 않지만, 이중 방어로 남겨둔다.
     */
    private void validatePageAndUser(Long userId, Long pageId) {
        if (!pageRepository.existsById(pageId)) {
            throw new CustomException(CuttonPageErrorCode.PAGE_NOT_FOUND);
        }
        if (!userRepository.existsById(userId)) {
            throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    /**
     * addBookmark/addLike 전용 - 페이지/유저 존재 확인에 더해, 그 유저가 이 페이지가 속한
     * 책을 내 도서 목록(reading_records)에 추가해뒀는지까지 확인한다. getPageContentForUser()/
     * getChapterContent()와 동일한 이유(IDOR 방지)로, validatePageAndUser()만으로는 소유하지
     * 않은 책의 페이지에도 반응(북마크/좋아요)을 저장할 수 있는 구멍이 있었다. 소유 여부를
     * 403 등으로 따로 알려주면 pageId를 대입해보는 것만으로 "이 페이지가 존재한다"는 정보가
     * 새어나가므로, 미존재 페이지와 동일하게 PAGE_NOT_FOUND(404)로 응답한다(테스트 우회
     * 유저 1~4는 예외).
     */
    private void validatePageOwnedByUser(Long userId, Long pageId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new CustomException(CuttonPageErrorCode.PAGE_NOT_FOUND));
        if (!userRepository.existsById(userId)) {
            throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!jwtUtil.isTestBypassUser(userId)
                && readingRecordRepository.findByUserIdAndBookId(userId, page.getBook().getBookId()).isEmpty()) {
            throw new CustomException(CuttonPageErrorCode.PAGE_NOT_FOUND);
        }
    }

    /**
     * getLikedPages() 전용 - cursorSavedAt/cursorId는 둘 다 있거나 둘 다 없는 경우만 허용한다
     * (ActivityService.validateCursorPair()와 동일한 이유 - createdAt이 DATE라 같은 날짜 항목이
     * 겹칠 수 있어 recordPageId를 2차 키로 같이 써야 순서/완전성이 보장된다).
     */
    private void validateCursorPair(LocalDate cursorSavedAt, Long cursorId) {
        if ((cursorSavedAt == null) != (cursorId == null)) {
            throw new CustomException(CuttonPageErrorCode.INVALID_CURSOR);
        }
    }

    /**
     * Authorization 헤더("Bearer {token}")에서 access token을 추출하고 검증하여 userId를 반환한다.
     * (UserService의 같은 이름 메서드와 동일한 방식 - 테스트 우회 토큰 1~4도 JwtUtil에서 그대로 처리됨)
     */
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

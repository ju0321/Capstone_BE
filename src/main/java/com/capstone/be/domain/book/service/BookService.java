package com.capstone.be.domain.book.service;

import com.capstone.be.domain.book.dto.pdf.BookJsonFileDto;
import com.capstone.be.domain.book.dto.pdf.BookJsonImageDto;
import com.capstone.be.domain.book.dto.pdf.BookJsonPageDto;
import com.capstone.be.domain.book.dto.request.BookJsonCreateRequest;
import com.capstone.be.domain.book.dto.response.BookCreateResponse;
import com.capstone.be.domain.book.dto.response.BookDetailResponse;
import com.capstone.be.domain.book.dto.response.BookSearchResponse;
import com.capstone.be.domain.book.dto.response.GenreListItemResponse;
import com.capstone.be.domain.book.dto.response.GenreResponse;
import com.capstone.be.domain.book.dto.response.PopularBooksResponse;
import com.capstone.be.domain.book.entity.Book;
import com.capstone.be.domain.book.entity.Page;
import com.capstone.be.domain.book.entity.Sentence;
import com.capstone.be.domain.book.enums.SentenceStatus;
import com.capstone.be.domain.book.error.BookErrorCode;
import com.capstone.be.domain.book.repository.BookRepository;
import com.capstone.be.domain.book.repository.GenreRepository;
import com.capstone.be.domain.book.repository.PageRepository;
import com.capstone.be.domain.book.repository.SentenceRepository;
import com.capstone.be.global.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final PageRepository pageRepository;
    private final SentenceRepository sentenceRepository;
    private final GenreRepository genreRepository;
    private final ObjectMapper objectMapper;

    private static final int SEARCH_RESULT_LIMIT = 20;

    /**
     * FastAPI(yeobaek-python)가 만든 book_Json 파일 + 책 메타데이터를 받아서
     * Book -> Page -> Sentence 순서로 저장한다.
     * <p>
     * 문장/이미지 순서 관련 주의사항: book_Json에는 sentences와 images가 페이지별로
     * 각각 별도 배열로 들어있어서, 실제 원본 문서에서 문장과 이미지가 어떤 순서로
     * 섞여 있었는지는 알 수 없다. 그래서 지금은 "한 페이지 안에서 이미지를 먼저(자신들의
     * order 필드 기준으로 정렬) 넣고, 그다음 문장을 순서대로 이어붙이는" 임시 방식으로
     * sentenceIndex를 매긴다. 실제로는 텍스트 페이지 아니면 이미지 페이지인 경우가
     * 대부분이라 크게 문제되지 않지만, 두 가지가 한 페이지에 정말 섞여 있는 경우의
     * 정확한 순서가 필요해지면 파이썬 쪽에서 통합된 순서 정보를 함께 내려주도록
     * 개선해야 한다.
     */
    @Transactional
    public BookCreateResponse createBookJson(BookJsonCreateRequest request, String coverImageUrl, MultipartFile bookJsonFile) {

        BookJsonFileDto bookJson = parseBookJsonFile(bookJsonFile);

        // genreId는 필수가 아니지만, 들어왔다면 실제로 존재하는 장르인지 먼저 확인한다.
        // 확인 없이 그대로 저장하면 books.genre_id의 FK 제약에 걸려 DataIntegrityViolationException으로
        // 500 에러가 나버리므로, 깔끔한 404(GENRE_NOT_FOUND)로 미리 막는다. (BookController에서도
        // 커버 이미지를 S3에 올리기 전에 한 번 더 미리 검증한다 - 아래 validateGenreId() 참고)
        validateGenreId(request.getGenreId());

        Book book = bookRepository.save(
                Book.builder()
                        .title(request.getTitle())
                        .author(request.getAuthor())
                        .publisher(request.getPublisher())
                        .description(request.getDescription())
                        .genreId(request.getGenreId())
                        .createdAt(request.getCreatedAt())
                        .coverImageUrl(coverImageUrl)
                        .build()
        );

        List<BookJsonPageDto> pages = bookJson.getPages() != null ? bookJson.getPages() : List.of();

        // book_Json의 pages 배열에 null 요소가 섞여 있으면(예: {"pages":[null]}) 잘못된
        // 형식의 파일로 간주하고 저장 전에 막는다. 이 검사가 없으면 아래 for문에서
        // pageDto.getPageNumber() 호출 시 NPE가 발생하면서 INVALID_BOOK_JSON 처리 없이
        // 그대로 500 에러로 이어진다.
        if (pages.contains(null)) {
            throw new CustomException(BookErrorCode.INVALID_BOOK_JSON);
        }

        int savedSentenceCount = 0;
        int savedImageCount = 0;

        for (BookJsonPageDto pageDto : pages) {

            Page page = pageRepository.save(
                    Page.builder()
                            .book(book)
                            .pageNumber(pageDto.getPageNumber())
                            .chapter(pageDto.getChapter())
                            // 페이지 전체 스캔 이미지는 이 API 범위 밖 (개별 삽화는 Sentence.status=IMG로 다룸)
                            .build()
            );

            List<Sentence> sentencesToSave = new ArrayList<>();
            int index = 0;

            // 이미지를 먼저 넣고 그다음 문장을 넣는다. 한 페이지에 이미지와 문장이 섞여 있을 때
            // 정확히 몇 번째 문장 사이에 이미지가 끼어드는지는 book_Json만으로 알 수 없어서,
            // "이미지 전부 -> 문장 전부" 순서로 임시 배치한다. 이미지가 여러 장이면 각 이미지의
            // order 필드 기준으로 정렬해서 이미지끼리의 순서는 정확하게 유지한다.
            List<BookJsonImageDto> images = pageDto.getImages() != null ? pageDto.getImages() : List.of();
            List<BookJsonImageDto> sortedImages = images.stream()
                    .sorted(Comparator.comparing(
                            BookJsonImageDto::getOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            for (BookJsonImageDto imageDto : sortedImages) {
                // TODO: 지금은 이미지 실제 파일(바이너리)이 이 API로 전달되지 않아서 filename만
                //  임시로 저장한다. 추후 book_Json과 함께 이미지 파일들도 전달받아 실제
                //  스토리지에 업로드하고 그 URL을 저장하도록 개선해야 한다.
                sentencesToSave.add(
                        Sentence.builder()
                                .page(page)
                                .sentenceIndex(index++)
                                .content(imageDto.getFilename())
                                .status(SentenceStatus.IMG)
                                .build()
                );
            }

            List<String> sentenceTexts = pageDto.getSentences() != null ? pageDto.getSentences() : List.of();
            for (String sentenceText : sentenceTexts) {
                sentencesToSave.add(
                        Sentence.builder()
                                .page(page)
                                .sentenceIndex(index++)
                                .content(sentenceText)
                                .status(SentenceStatus.SENTENCE)
                                .build()
                );
            }

            sentenceRepository.saveAll(sentencesToSave);
            savedSentenceCount += sentenceTexts.size();
            savedImageCount += images.size();
        }

        return BookCreateResponse.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .totalPages(bookJson.getTotalPages())
                .savedSentenceCount(savedSentenceCount)
                .savedImageCount(savedImageCount)
                .build();
    }

    /**
     * 제목/저자/출판사에 검색어가 포함된 책을 관련도순으로 최대 SEARCH_RESULT_LIMIT권까지 반환한다.
     * (정렬 우선순위는 BookRepository.searchByKeyword() 쿼리 참고)
     * <p>
     * TODO: 지금은 상위 20권만 한 번에 반환한다. 검색 결과가 20권을 넘어갈 때 추가로
     *  더 보여줄 방법(스크롤을 내리면 이어서 불러오는 무한 스크롤 방식인지, "더보기"
     *  버튼을 눌러야 다음 목록을 가져오는 방식인지)이 프론트 쪽에서 정해지면, 그에 맞춰
     *  실제 페이징(offset/cursor 기반 다음 페이지 조회) 파라미터와 응답을 추가해야 한다.
     */
    @Transactional(readOnly = true)
    public List<BookSearchResponse> searchBooks(String keyword) {
        String escapedKeyword = escapeLikeKeyword(keyword);
        List<Book> books = bookRepository.searchByKeyword(escapedKeyword, PageRequest.of(0, SEARCH_RESULT_LIMIT));

        return books.stream()
                .map(book -> BookSearchResponse.builder()
                        .bookId(book.getBookId())
                        .title(book.getTitle())
                        .author(book.getAuthor())
                        .publisher(book.getPublisher())
                        .coverImageUrl(book.getCoverImageUrl())
                        .createdAt(book.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * bookId로 책의 상세 정보를 조회한다. 해당 책이 없으면 BookErrorCode.BOOK_NOT_FOUND 예외를 던진다.
     * <p>
     * 장르는 Book.genreId로 genres 테이블에서 type/value를 조회해서 genreList로 내려준다.
     * 아직 책 하나에 장르를 하나만 매길 수 있는 구조라(genre_id 단일 FK) 0개 또는 1개짜리
     * 목록이지만, 응답 계약을 List로 둬서 추후 책 하나에 장르를 여러 개 매길 수 있게 되더라도
     * 응답 형태를 바꾸지 않아도 되게 했다.
     */
    @Transactional(readOnly = true)
    public BookDetailResponse getBookInfo(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(BookErrorCode.BOOK_NOT_FOUND));

        List<GenreResponse> genreList = (book.getGenreId() != null)
                ? genreRepository.findById(book.getGenreId())
                        .map(genre -> List.of(GenreResponse.builder()
                                .type(genre.getType().name())
                                .value(genre.getValue())
                                .build()))
                        .orElse(List.of())
                : List.of();

        return BookDetailResponse.builder()
                .title(book.getTitle())
                .author(book.getAuthor())
                .coverImageUrl(book.getCoverImageUrl())
                .description(book.getDescription())
                .genreList(genreList)
                .publisher(book.getPublisher())
                .createdAt(book.getCreatedAt())
                .build();
    }

    /**
     * genreId가 실제로 존재하는 장르인지 확인한다(genreId가 null이면 장르 없이 등록하는 것으로
     * 보고 통과시킨다). 존재하지 않으면 GENRE_NOT_FOUND(404)를 던진다.
     * <p>
     * BookController.createBookJson()이 커버 이미지를 S3에 업로드하기 전에 먼저 호출한다 -
     * 검증 없이 업로드부터 하면, 잘못된 genreId 때문에 책 저장이 실패해도 이미 올라간 커버
     * 이미지가 S3에 고아 오브젝트로 남기 때문이다(DB 트랜잭션 롤백은 S3 업로드를 되돌리지 못함).
     */
    @Transactional(readOnly = true)
    public void validateGenreId(Long genreId) {
        if (genreId != null && !genreRepository.existsById(genreId)) {
            throw new CustomException(BookErrorCode.GENRE_NOT_FOUND);
        }
    }

    /**
     * genres 테이블에 저장된 전체 장르(genreId/type/value)를 그대로 반환한다. 관리자용 API -
     * 책 등록("JSON형태 책 저장하기") 시 넣을 genreId를 고르기 위해 어떤 장르가 있는지 확인하는
     * 용도다.
     */
    @Transactional(readOnly = true)
    public List<GenreListItemResponse> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(genre -> GenreListItemResponse.builder()
                        .genreId(genre.getGenreId())
                        .type(genre.getType().name())
                        .value(genre.getValue())
                        .build())
                .toList();
    }

    /**
     * "인기 도서" 목록을 20권씩 페이징해서 반환한다.
     * <p>
     * TODO: /api/v1/books/popular 를 추천 도서 기준으로 뜨도록 수정 예정 - 조회수/좋아요 같은
     *  일반 인기도 지표보다 먼저, 추천 도서 로직(결과)을 기준으로 정렬/구성하도록 바꾼다.
     * <p>
     * TODO: 아직 조회수/좋아요 같은 실제 인기도 데이터가 없어서, 지금은 전체 책 중에서
     *  무작위 순서로 채운다(BookRepository.findRandomBooks() 참고). 나중에 viewCount 등
     *  실제 지표가 쌓이면 그 기준으로 정렬하도록 바꿔야 한다. 또한 ORDER BY RANDOM()은
     *  요청마다 순서가 다시 섞이기 때문에, 페이지를 넘길 때 책이 중복되거나 누락될 수
     *  있다는 한계가 있다 - 지금은 책이 2권뿐이라 문제되지 않지만, 실제 인기도 정렬로
     *  바뀌면 이 문제도 자연히 해결된다.
     */
    @Transactional(readOnly = true)
    public PopularBooksResponse getPopularBooks(int page, int size) {
        org.springframework.data.domain.Page<Book> bookPage = bookRepository.findRandomBooks(PageRequest.of(page, size));

        List<BookSearchResponse> books = bookPage.getContent().stream()
                .map(book -> BookSearchResponse.builder()
                        .bookId(book.getBookId())
                        .title(book.getTitle())
                        .author(book.getAuthor())
                        .publisher(book.getPublisher())
                        .coverImageUrl(book.getCoverImageUrl())
                        .createdAt(book.getCreatedAt())
                        .build())
                .toList();

        return PopularBooksResponse.builder()
                .books(books)
                .page(page)
                .size(size)
                .totalElements(bookPage.getTotalElements())
                .totalPages(bookPage.getTotalPages())
                .hasNext(bookPage.hasNext())
                .build();
    }

    /**
     * 검색어에 포함된 SQL LIKE 와일드카드 문자(% _)와 이스케이프 문자(\\)를
     * 리터럴로 취급하도록 이스케이프한다. 반드시 백슬래시(\\)를 가장 먼저
     * 이스케이프해야 그 뒤에 추가하는 이스케이프 문자와 충돌하지 않는다.
     * BookRepository.searchByKeyword() 쿼리의 ESCAPE '\\' 절과 짝을 이룬다.
     */
    private String escapeLikeKeyword(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * 업로드된 book_Json 파일을 읽어서 DTO로 파싱한다.
     */
    private BookJsonFileDto parseBookJsonFile(MultipartFile bookJsonFile) {
        try {
            return objectMapper.readValue(bookJsonFile.getInputStream(), BookJsonFileDto.class);
        } catch (IOException e) {
            throw new CustomException(BookErrorCode.INVALID_BOOK_JSON);
        }
    }
}

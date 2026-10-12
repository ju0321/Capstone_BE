package com.capstone.be.domain.book_sentences.service;

import com.capstone.be.domain.book.entity.Sentence;
import com.capstone.be.domain.book.error.BookErrorCode;
import com.capstone.be.domain.book.repository.SentenceRepository;
import com.capstone.be.domain.book_sentences.dto.request.SentenceHighlightColorUpdateRequest;
import com.capstone.be.domain.book_sentences.dto.request.SentenceHighlightCreateRequest;
import com.capstone.be.domain.book_sentences.dto.request.SentenceHighlightMemoUpdateRequest;
import com.capstone.be.domain.book_sentences.dto.response.SentenceHighlightListItemResponse;
import com.capstone.be.domain.book_sentences.dto.response.SentenceHighlightResponse;
import com.capstone.be.domain.book_sentences.entity.SentenceHighlight;
import com.capstone.be.domain.book_sentences.enums.HighlightColor;
import com.capstone.be.domain.book_sentences.error.HighlightErrorCode;
import com.capstone.be.domain.book_sentences.repository.HighlightListProjection;
import com.capstone.be.domain.book_sentences.repository.HighlightRepository;
import com.capstone.be.global.exception.CustomException;
import com.capstone.be.global.exception.GlobalErrorCode;
import com.capstone.be.global.util.JwtUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SentenceService {

    private final HighlightRepository highlightRepository;
    private final SentenceRepository sentenceRepository;
    private final JwtUtil jwtUtil;

    /**
     * 문장 수집(하이라이트 생성/갱신). 유저 한 명당 문장 하나에는 하이라이트가 하나만 존재하므로
     * (user_id, sentence_id) 조합이 이미 있으면 색/내용만 갱신하고, 없으면 새로 만든다(upsert).
     * sentenceId가 실제 존재하는 문장인지 먼저 확인하고, 요청받은 content가 그 문장 원문의
     * 부분 문자열인지 검증한다 - 그렇지 않으면 sentenceId만 유효하면 아무 텍스트나 하이라이트로
     * 저장할 수 있게 되어 버린다(응답/목록 API가 저장된 값을 그대로 돌려주므로 데이터 무결성
     * 문제로 이어진다).
     */
    @Transactional
    public SentenceHighlightResponse createHighlight(String authorizationHeader, SentenceHighlightCreateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        Long sentenceId = request.getSentenceId();

        Sentence sentence = sentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new CustomException(BookErrorCode.SENTENCE_NOT_FOUND));
        validateContentWithinSentence(sentence, request.getContent());

        highlightRepository.upsertHighlight(
                userId, sentenceId, request.getColor().name(), request.getContent());

        return toResponse(getExistingHighlight(userId, sentenceId));
    }

    /**
     * 드래그로 선택한 content가 실제 sentence 원문의 부분 문자열(또는 전체)인지 검증한다.
     * SentenceHighlight의 설계 전제(드래그 선택 범위가 문장 하나를 벗어나지 않음)를 지키기
     * 위한 서버 측 검증 - 프론트가 보내는 sentenceId/content 조합을 그대로 신뢰하지 않는다.
     */
    private void validateContentWithinSentence(Sentence sentence, String requestedContent) {
        String originalContent = sentence.getContent();
        if (originalContent == null || !originalContent.contains(requestedContent)) {
            throw new CustomException(HighlightErrorCode.HIGHLIGHT_CONTENT_OUT_OF_RANGE);
        }
    }

    /**
     * 하이라이트 색상 변경. content는 그대로 두고 color만 바꾼다. 해당 유저가 그 문장을
     * 수집한 적이 없으면(=하이라이트가 없으면) HIGHLIGHT_NOT_FOUND를 던진다.
     */
    @Transactional
    public SentenceHighlightResponse updateHighlightColor(
            String authorizationHeader, Long sentenceId, SentenceHighlightColorUpdateRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        getExistingHighlight(userId, sentenceId);
        highlightRepository.updateColor(userId, sentenceId, request.getColor().name());

        return toResponse(getExistingHighlight(userId, sentenceId));
    }

    /**
     * 하이라이트 삭제. 해당 유저가 그 문장을 수집한 적이 없으면 HIGHLIGHT_NOT_FOUND를 던진다.
     */
    @Transactional
    public void deleteHighlight(String authorizationHeader, Long sentenceId) {
        Long userId = resolveUserId(authorizationHeader);

        getExistingHighlight(userId, sentenceId);
        highlightRepository.deleteByUserIdAndSentenceId(userId, sentenceId);
    }

    private SentenceHighlight getExistingHighlight(Long userId, Long sentenceId) {
        return highlightRepository.findByUserIdAndSentenceId(userId, sentenceId)
                .orElseThrow(() -> new CustomException(HighlightErrorCode.HIGHLIGHT_NOT_FOUND));
    }

    private SentenceHighlightResponse toResponse(SentenceHighlight highlight) {
        return SentenceHighlightResponse.builder()
                .sentenceId(highlight.getSentenceId())
                .content(highlight.getContent())
                .color(highlight.getColor())
                .createdAt(highlight.getCreatedAt())
                .build();
    }

    /**
     * 로그인한 유저가 수집한 문장 목록을 최신순으로 조회한다. bookId를 주면 그 책에서 수집한
     * 문장만 걸러서 보여준다(전체를 보고 싶으면 bookId 없이 호출).
     */
    @Transactional(readOnly = true)
    public List<SentenceHighlightListItemResponse> getMyHighlights(String authorizationHeader, Long bookId) {
        Long userId = resolveUserId(authorizationHeader);

        return highlightRepository.findAllByUserId(userId, bookId).stream()
                .map(this::toListItemResponse)
                .toList();
    }

    /**
     * 수집한 문장 하나의 상세보기. 책/페이지 컨텍스트 + 메모까지 함께 반환한다.
     */
    @Transactional(readOnly = true)
    public SentenceHighlightListItemResponse getHighlightDetail(String authorizationHeader, Long sentenceId) {
        Long userId = resolveUserId(authorizationHeader);

        return highlightRepository.findDetailByUserIdAndSentenceId(userId, sentenceId)
                .map(this::toListItemResponse)
                .orElseThrow(() -> new CustomException(HighlightErrorCode.HIGHLIGHT_NOT_FOUND));
    }

    /**
     * 수집한 문장의 메모를 작성/수정한다. memo가 비어있으면(null/빈 문자열) 메모를 지운다.
     */
    @Transactional
    public SentenceHighlightListItemResponse updateHighlightMemo(
            String authorizationHeader, Long sentenceId, SentenceHighlightMemoUpdateRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        getExistingHighlight(userId, sentenceId);
        highlightRepository.updateMemo(userId, sentenceId, request.getMemo());

        return highlightRepository.findDetailByUserIdAndSentenceId(userId, sentenceId)
                .map(this::toListItemResponse)
                .orElseThrow(() -> new CustomException(HighlightErrorCode.HIGHLIGHT_NOT_FOUND));
    }

    private SentenceHighlightListItemResponse toListItemResponse(HighlightListProjection projection) {
        return SentenceHighlightListItemResponse.builder()
                .sentenceId(projection.getSentenceId())
                .content(projection.getContent())
                .color(HighlightColor.valueOf(projection.getColor()))
                .memo(projection.getMemo())
                .createdAt(projection.getCreatedAt())
                .bookId(projection.getBookId())
                .bookTitle(projection.getBookTitle())
                .pageId(projection.getPageId())
                .pageNumber(projection.getPageNumber())
                .chapter(projection.getChapter())
                .build();
    }

    /**
     * Authorization 헤더("Bearer {token}")에서 access token을 추출하고 검증하여 userId를 반환한다.
     * (CuttonPageService의 같은 이름 메서드와 동일한 방식)
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

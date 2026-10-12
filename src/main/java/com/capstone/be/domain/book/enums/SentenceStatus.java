package com.capstone.be.domain.book.enums;

/**
 * Sentence.content 가 실제 문장 텍스트인지, 이미지 URL인지 구분하는 값.
 * 문장과 이미지를 한 페이지 안에서 등장 순서(sentenceIndex) 그대로 섞어서 저장하기 위한 필드.
 */
public enum SentenceStatus {
    SENTENCE,
    IMG
}

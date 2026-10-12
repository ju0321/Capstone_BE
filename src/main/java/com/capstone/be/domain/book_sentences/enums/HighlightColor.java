package com.capstone.be.domain.book_sentences.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 문장 수집(하이라이트) 시 선택할 수 있는 형광펜 색상.
 */
@Getter
@AllArgsConstructor
public enum HighlightColor {
    YELLOW("#E6E19E"),
    GREEN("#C7D6D3"),
    BLUE("#C5CEE4"),
    PINK("#D4C7D6"),
    GRAY("#CECECE")
    ;

    private final String hexCode;
}

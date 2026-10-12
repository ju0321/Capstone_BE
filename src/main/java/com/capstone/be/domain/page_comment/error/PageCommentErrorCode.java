package com.capstone.be.domain.page_comment.error;

import com.capstone.be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PageCommentErrorCode implements BaseErrorCode {

    // 페이지 존재 여부는 cutton_page 도메인의 CuttonPageErrorCode.PAGE_NOT_FOUND를 그대로 사용한다.
    SENTENCE_NOT_FOUND("PC001", "존재하지 않거나 해당 페이지에 속하지 않는 문장입니다.", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("PC002", "존재하지 않는 댓글입니다.", HttpStatus.NOT_FOUND),
    COMMENT_ACCESS_DENIED("PC003", "본인이 작성한 댓글만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
    REPLY_TO_REPLY_NOT_ALLOWED("PC004", "답글에는 답글을 달 수 없습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_LIKED("PC005", "이미 좋아하는 댓글입니다.", HttpStatus.CONFLICT),
    ALREADY_DISLIKED("PC006", "이미 싫어하는 댓글입니다.", HttpStatus.CONFLICT),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}

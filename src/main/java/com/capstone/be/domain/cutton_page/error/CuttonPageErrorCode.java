package com.capstone.be.domain.cutton_page.error;

import com.capstone.be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CuttonPageErrorCode implements BaseErrorCode {

    // 소유하지 않은 책의 페이지/챕터를 조회할 때도 존재하지 않는 페이지와 동일하게 이 코드(404)를
    // 던진다 - 403(BOOK_NOT_OWNED)과 구분해서 내려주면 pageId를 대입해보는 것만으로 "이 페이지가
    // 존재는 한다"는 정보가 새어나가기 때문에(CWE-203), 소유 여부와 무관하게 항상 같은 404 응답으로
    // 통일한다.
    PAGE_NOT_FOUND("CP001", "존재하지 않는 페이지입니다.", HttpStatus.NOT_FOUND),

    // getLikedPages()의 커서(cursorSavedAt, cursorId)도 activity 쪽과 동일한 이유로 둘 다
    // 있거나 둘 다 없는 경우만 허용한다(ActivityErrorCode.INVALID_CURSOR와 동일한 목적).
    INVALID_CURSOR("CP002", "cursorSavedAt과 cursorId는 함께 보내거나 함께 생략해야 합니다.", HttpStatus.BAD_REQUEST),

    // "책 전체 내용 가져오기"에서 쓴다. 소유하지 않은 책을 조회할 때도 존재하지 않는 책과 동일하게
    // 이 코드(404)를 던진다 - 위 PAGE_NOT_FOUND와 동일한 이유(IDOR/정보 노출 방지)다.
    BOOK_NOT_FOUND("CP003", "존재하지 않는 책입니다.", HttpStatus.NOT_FOUND),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}

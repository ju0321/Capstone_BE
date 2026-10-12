package com.capstone.be.domain.page_comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PageCommentCreateRequest {

    // 특정 문장에 다는 댓글이면 그 문장의 sentenceId를, 페이지 전체에 다는 댓글이면 생략(null)한다.
    // null이면 PageCommentService.createComment()에서 문장 조회/소속 검증을 건너뛰고 sentence
    // 없이(페이지 단위 댓글로) 저장한다.
    private Long sentenceId;

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;

    // status(스포일러 여부)는 클라이언트가 알 수 없는 값이라 요청에서 받지 않는다.
    // 작성 시점엔 항상 COMMON으로 저장되고, 추후 AI가 content를 읽어 스포일러 여부를 판단해
    // 갱신하는 로직이 붙을 예정이다.
}

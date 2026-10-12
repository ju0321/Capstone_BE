package com.capstone.be.domain.page_comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PageCommentUpdateRequest {
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;

    // status(스포일러 여부)는 클라이언트가 판단할 수 없는 값이라 수정 요청에서도 받지 않는다.
    // 수정 시엔 기존 상태를 그대로 유지하고, 추후 AI가 content 변경분을 다시 판단하는 로직이 붙을 예정이다.
}

package com.capstone.be.domain.page_comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PageCommentReplyCreateRequest {

    @NotBlank(message = "답글 내용을 입력해주세요.")
    private String content;

    // sentenceId는 원댓글의 sentenceId를 그대로 물려받아서 요청에서 따로 받지 않는다.
    // status(스포일러 여부)도 원댓글과 마찬가지로 클라이언트가 판단할 수 없어 항상 COMMON으로 저장한다.
}

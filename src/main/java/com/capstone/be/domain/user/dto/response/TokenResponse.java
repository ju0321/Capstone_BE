package com.capstone.be.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenResponse {

    private Long userId;
    private String nickname;
    private String accessToken;
    private String refreshToken;
    // TODO: 이미지 받은 후 이미지도 함께 반환하도록 추가하기
}

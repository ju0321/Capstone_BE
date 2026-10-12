package com.capstone.be.domain.user.controller;

import com.capstone.be.domain.user.dto.request.LoginRequest;
import com.capstone.be.domain.user.dto.request.SignupRequest;
import com.capstone.be.domain.user.dto.response.TokenResponse;
import com.capstone.be.domain.user.service.UserService;
import com.capstone.be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User", description = "사용자 관련 api")
public class UserController {

    private final UserService userService;

    // 회원가입 (성공 시 자동 로그인 - 토큰 함께 반환)
    @PostMapping("/signup")
    @Operation(summary = "앱 자체 회원가입", description = "..")
    public ResponseEntity<BaseResponse<TokenResponse>> signup(@Valid @RequestBody SignupRequest request) {
        TokenResponse response = userService.signup(request);
        return ResponseEntity.ok(BaseResponse.success("회원가입이 완료되었습니다.", response));
    }

    // 로그인
    @PostMapping("/login")
    @Operation(summary = "앱 자체 로그인", description = "..")
    public ResponseEntity<BaseResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = userService.login(request);
        return ResponseEntity.ok(BaseResponse.success("로그인이 완료되었습니다.", response));
    }

    // 간이 로그인 (정식 회원가입 화면 나오기 전 임시용 - 처음 보는 닉네임이면 자동 가입 후 로그인,
    // 이미 있는 닉네임이면 비밀번호가 일치해야 로그인됨)
    @PostMapping("/simple-login")
    @Operation(summary = "간이 로그인 (닉네임 기반, 자동 회원가입)",
            description = "처음 보는 닉네임이면 입력한 비밀번호로 새 계정을 만들어 바로 로그인시킵니다. "
                    + "이미 있는 닉네임이면 일반 로그인과 동일하게 비밀번호가 일치해야만 그 계정으로 로그인됩니다.")
    public ResponseEntity<BaseResponse<TokenResponse>> simpleLogin(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = userService.simpleLogin(request);
        return ResponseEntity.ok(BaseResponse.success("로그인이 완료되었습니다.", response));
    }

    // 로그아웃 (Authorization 헤더의 access token 검증 후, 실제 토큰 폐기는 클라이언트가 Keychain에서 처리)
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "..")
    public ResponseEntity<BaseResponse<Void>> logout(@RequestHeader("Authorization") String authorization) {
        userService.logout(authorization);
        return ResponseEntity.ok(BaseResponse.success("로그아웃이 완료되었습니다.", null));
    }

    // 닉네임 중복 확인 (사용 가능하면 200, 이미 사용 중이면 409)
    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 확인", description = "회원가입 시 사용")
    public ResponseEntity<BaseResponse<Void>> checkNickname(
            @RequestParam @NotBlank(message = "닉네임을 입력해주세요.") String nickname) {
        userService.checkNicknameDuplicate(nickname);
        return ResponseEntity.ok(BaseResponse.success("사용 가능한 닉네임입니다.", null));
    }
}

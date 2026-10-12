package com.capstone.be.domain.user.service;

import com.capstone.be.domain.user.dto.request.LoginRequest;
import com.capstone.be.domain.user.dto.request.SignupRequest;
import com.capstone.be.domain.user.dto.response.TokenResponse;
import com.capstone.be.domain.user.enums.AuthProvider;
import com.capstone.be.domain.user.entity.User;
import com.capstone.be.domain.user.error.UserErrorCode;
import com.capstone.be.domain.user.repository.UserRepository;
import com.capstone.be.global.exception.CustomException;
import com.capstone.be.global.exception.GlobalErrorCode;
import com.capstone.be.global.util.JwtUtil;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * simpleLogin()으로 자동 생성되는 계정의 email/birth placeholder. email/birth는 users
     * 테이블에 NOT NULL(email은 UNIQUE)이라 비워둘 수 없는데, 프론트에 아직 회원가입 화면이
     * 없어서 실제 값을 받을 방법이 없다. 이메일은 "닉네임@simple.capstone.local"로 만들어서
     * 유일성을 닉네임의 유일성(DB unique 제약)에 그대로 얹는다.
     */
    private static final String SIMPLE_LOGIN_EMAIL_DOMAIN = "@simple.capstone.local";
    private static final LocalDate SIMPLE_LOGIN_PLACEHOLDER_BIRTH = LocalDate.of(2000, 1, 1);

    /**
     * 회원가입을 처리합니다. (닉네임/이메일 중복 검증, 비밀번호 암호화 후 저장, 가입과 동시에 자동 로그인 토큰 발급)
     */
    @Transactional
    public TokenResponse signup(SignupRequest request) {

        // 비밀번호 / 비밀번호 확인 일치 검증
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CustomException(UserErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        // 닉네임 중복 검증 (닉네임이 로그인 식별자 역할을 겸함)
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(UserErrorCode.NICKNAME_DUPLICATED);
        }

        // 이메일 중복 검증
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(UserErrorCode.EMAIL_DUPLICATED);
        }

        User user = User.builder()
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .birth(request.getBirth())
                .email(request.getEmail())
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .profileImageUrl(null) // 기본 이미지 미정 - 추후 기본 이미지 URL 적용 예정
                .build();

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 대비: existsBy 검증을 통과해도 DB unique 제약에서 최종적으로 걸러질 수 있음
            String detail = e.getMostSpecificCause().getMessage();
            if (detail != null && detail.contains("nickname")) {
                throw new CustomException(UserErrorCode.NICKNAME_DUPLICATED);
            }
            if (detail != null && detail.contains("email")) {
                throw new CustomException(UserErrorCode.EMAIL_DUPLICATED);
            }
            throw e;
        }

        // 회원가입과 동시에 자동 로그인 처리 - 토큰 발급
        return buildTokenResponse(savedUser);
    }

    /**
     * 닉네임/비밀번호 기반 일반 로그인을 처리합니다. (소셜 로그인 제외) 자격증명 검증 후 토큰을 발급합니다.
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {

        // 닉네임으로 회원 조회 (계정 존재 여부가 노출되지 않도록 비밀번호 불일치와 동일한 에러코드 사용)
        User user = userRepository.findByNickname(request.getNickname())
                .orElseThrow(() -> new CustomException(UserErrorCode.LOGIN_FAILED));

        // 비밀번호 일치 검증 (암호화된 값끼리 비교)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(UserErrorCode.LOGIN_FAILED);
        }

        return buildTokenResponse(user);
    }

    /**
     * 정식 회원가입 화면이 아직 없는 상태에서 쓰는 간이 로그인입니다. 처음 보는 닉네임이면
     * (입력한 비밀번호로) 새로 계정을 만들어 바로 로그인시키고, 이미 있는 닉네임이면 일반
     * login()과 동일하게 비밀번호가 일치해야만 그 계정으로 로그인됩니다 - 즉 닉네임만 겹치지
     * 않으면 되고, 회원가입 절차(이메일/생년월일 입력 등) 없이 닉네임을 로그인 식별자로 그대로
     * 씁니다.
     */
    @Transactional
    public TokenResponse simpleLogin(LoginRequest request) {

        return userRepository.findByNickname(request.getNickname())
                .map(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        throw new CustomException(UserErrorCode.LOGIN_FAILED);
                    }
                    return buildTokenResponse(user);
                })
                .orElseGet(() -> buildTokenResponse(createSimpleUser(request)));
    }

    private User createSimpleUser(LoginRequest request) {
        User user = User.builder()
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .birth(SIMPLE_LOGIN_PLACEHOLDER_BIRTH)
                .email(request.getNickname() + SIMPLE_LOGIN_EMAIL_DOMAIN)
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .profileImageUrl(null)
                .build();

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은(새) 닉네임으로 요청이 들어와 존재 여부 조회 시점엔 없었지만 저장 시점에
            // 이미 다른 요청이 먼저 만들어버린 경우 - signup()과 동일한 패턴으로 처리한다.
            throw new CustomException(UserErrorCode.NICKNAME_DUPLICATED);
        }
    }

    /**
     * 로그아웃을 처리합니다. access token은 Redis 블랙리스트에 등록하고 refresh token은 삭제하여
     * 만료 전이라도 즉시 무효화합니다. (단, 테스트 우회 유저 1~4는 계속 재사용할 수 있도록 제외합니다.)
     */
    public void logout(String authorizationHeader) {

        String accessToken = extractAccessToken(authorizationHeader);
        // 토큰 유효성 검증 (위조/만료된 토큰이면 JwtUtil에서 INVALID_TOKEN 예외 발생)
        Long userId = jwtUtil.getUserIdFromToken(accessToken);
        jwtUtil.invalidateTokens(userId, accessToken);
    }

    /**
     * 닉네임 중복 여부를 확인합니다. 이미 사용 중이면 예외를 던집니다.
     */
    @Transactional(readOnly = true)
    public void checkNicknameDuplicate(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(UserErrorCode.NICKNAME_DUPLICATED);
        }
    }

    /**
     * Authorization 헤더("Bearer {token}")에서 access token 문자열만 추출합니다.
     */
    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }
        return authorizationHeader.substring("Bearer ".length());
    }

    /**
     * 인증(가입/로그인)에 성공한 User를 기반으로 토큰을 발급하고 응답을 구성합니다.
     */
    private TokenResponse buildTokenResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        return TokenResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}

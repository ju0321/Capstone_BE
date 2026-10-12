package com.capstone.be.domain.user.entity;

import com.capstone.be.domain.user.enums.AuthProvider;
import com.capstone.be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    @Column(name = "password",nullable = false)
    private String password;

    @Column(name = "birth",nullable = false)
    private LocalDate birth;

    @Column(name = "email",nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider",nullable = false, length = 50)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Builder
    public User(String nickname, String password, LocalDate birth,
                String email, AuthProvider provider, String providerId, String profileImageUrl) {
        this.nickname = nickname;
        this.password = password;
        this.birth = birth;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.profileImageUrl = profileImageUrl;
    }

    /** 사용자 이미지 등록/변경 API에서 S3 업로드 후 받은 URL로 프로필 이미지를 갱신한다. */
    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}

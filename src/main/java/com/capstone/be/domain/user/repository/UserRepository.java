package com.capstone.be.domain.user.repository;

import com.capstone.be.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    Optional<User> findByNickname(String nickname);
}

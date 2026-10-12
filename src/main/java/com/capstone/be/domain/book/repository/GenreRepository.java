package com.capstone.be.domain.book.repository;

import com.capstone.be.domain.book.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {
}

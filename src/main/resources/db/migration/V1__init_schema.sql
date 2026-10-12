CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    user_id           BIGSERIAL PRIMARY KEY,
    nickname          VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    birth             DATE NOT NULL,
    email             VARCHAR(255) NOT NULL UNIQUE,
    provider          VARCHAR(50) NOT NULL,
    provider_id       VARCHAR(255),
    profile_image_url VARCHAR(255),
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

INSERT INTO users (nickname, password, birth, email, provider, provider_id, profile_image_url, created_at, updated_at)
VALUES
    ('홍길동', '$2b$10$jz5sk8jzqei3sOU99WjgjezpD1VFeqoY/QN9hpZQCX5HRUueNS8j2', '2001-01-01', 'seohu@yeobaek.com', 'LOCAL', NULL, NULL, '2026-09-14 00:00:00', '2026-09-14 00:00:00'),
    ('오지우', '$2b$10$rLbY7b1rbPglOwsIl88iSe6q4P5FudkR2wCt42fHLDOS6nsj62LLC', '2003-01-01', 'jiwoo@yeobaek.com', 'LOCAL', NULL, NULL, '2026-09-14 00:00:00', '2026-09-14 00:00:00'),
    ('김길동', '$2b$10$MivGGJ3kYz9BwI26nmCva.pNDbUOtpAJ6WVyTlc5GntjNs8E.Uqnq', '2002-01-01', 'seungmin@yeobaek.com', 'LOCAL', NULL, NULL, '2026-09-14 00:00:00', '2026-09-14 00:00:00'),
    ('전길동', '$2b$10$mRqZVAr.R4uzcEQ7p1839uE.ZngILqmzURUeFn/SgPPx592yDCcAW', '2002-01-02', 'yeji@yeobaek.com', 'LOCAL', NULL, NULL, '2026-09-14 00:00:00', '2026-09-14 00:00:00');

CREATE TABLE genres (
    genre_id BIGSERIAL PRIMARY KEY,
    type     VARCHAR(50)  NOT NULL,
    value    VARCHAR(100) NOT NULL
);

INSERT INTO genres (type, value) VALUES
    ('소설', '한국소설'),
    ('소설', '외국소설'),
    ('소설', '추리'),
    ('소설', '로맨스'),
    ('소설', 'SF'),
    ('소설', '판타지'),
    ('인문', '철학'),
    ('인문', '심리학'),
    ('인문', '종교'),
    ('인문', '역사'),
    ('인문', '문화'),
    ('경제_경영', '재테크'),
    ('경제_경영', '투자'),
    ('경제_경영', '창업'),
    ('경제_경영', '마케팅'),
    ('경제_경영', '경영'),
    ('자기계발', '습관'),
    ('자기계발', '인간관계'),
    ('자기계발', '시간관리'),
    ('자기계발', '성공'),
    ('사회_정치', '사회문제'),
    ('사회_정치', '정치'),
    ('사회_정치', '법'),
    ('사회_정치', '교육'),
    ('과학', '자연과학'),
    ('과학', '생명과학'),
    ('과학', '우주'),
    ('과학', 'IT·기술'),
    ('예술_대중문화', '미술'),
    ('예술_대중문화', '음악'),
    ('예술_대중문화', '영화'),
    ('예술_대중문화', '사진'),
    ('예술_대중문화', '디자인'),
    ('컴퓨터_IT', '프로그래밍'),
    ('컴퓨터_IT', 'AI'),
    ('컴퓨터_IT', '데이터'),
    ('컴퓨터_IT', '보안'),
    ('여행_취미', '여행'),
    ('여행_취미', '요리'),
    ('여행_취미', '운동'),
    ('여행_취미', '취미'),
    ('에세이', '감성 에세이'),
    ('에세이', '삶'),
    ('에세이', '관계'),
    ('에세이', '일상'),
    ('시_희곡', '시집'),
    ('시_희곡', '희곡'),
    ('시_희곡', '문학');

CREATE TABLE books (
    book_id         BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    author          VARCHAR(100) NOT NULL,
    cover_image_url TEXT,
    description     TEXT,
    publisher       VARCHAR(100) NOT NULL,
    created_at      DATE,
    genre_id        BIGINT REFERENCES genres (genre_id)
);

CREATE TABLE cutton_pages (
    page_id     BIGSERIAL PRIMARY KEY,
    book_id     BIGINT NOT NULL REFERENCES books (book_id),
    page_number INTEGER NOT NULL,
    image_url   TEXT,
    chapter     VARCHAR(255)
);

CREATE INDEX idx_cutton_pages_book_id ON cutton_pages (book_id);

CREATE TABLE sentences (
    sentence_id    BIGSERIAL PRIMARY KEY,
    page_id        BIGINT NOT NULL REFERENCES cutton_pages (page_id),
    sentence_index INTEGER,
    content        TEXT,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_sentences_page_id ON sentences (page_id);

CREATE TABLE book_page_status (
    record_page_id BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    page_id        BIGINT NOT NULL REFERENCES cutton_pages (page_id),
    type           VARCHAR(20) NOT NULL,
    created_at     DATE,
    CONSTRAINT book_page_status_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT uk_book_page_status_page_user_type UNIQUE (page_id, user_id, type)
);

CREATE INDEX idx_book_page_status_page_id ON book_page_status (page_id);

CREATE TABLE page_comments (
    comment_id            BIGSERIAL PRIMARY KEY,
    user_id               BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    page_id               BIGINT NOT NULL REFERENCES cutton_pages (page_id) ON DELETE CASCADE,
    sentence_id           BIGINT REFERENCES sentences (sentence_id) ON DELETE CASCADE,
    content               TEXT,
    created_at            TIMESTAMP NOT NULL,
    status                VARCHAR(20) NOT NULL,
    parent_comment_id     BIGINT REFERENCES page_comments (comment_id) ON DELETE CASCADE,
    perspective_embedding vector(768)
);

CREATE INDEX idx_page_comments_page_id ON page_comments (page_id);
CREATE INDEX idx_page_comments_parent_comment_id ON page_comments (parent_comment_id);

CREATE TABLE comment_reactions (
    comment_reaction_id BIGSERIAL PRIMARY KEY,
    comment_id          BIGINT NOT NULL REFERENCES page_comments (comment_id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    type                VARCHAR(20) NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    CONSTRAINT uk_comment_reaction_comment_user UNIQUE (comment_id, user_id)
);

CREATE INDEX idx_comment_reactions_comment_id ON comment_reactions (comment_id);

CREATE TABLE reading_records (
    record_id      BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    book_id        BIGINT NOT NULL REFERENCES books (book_id) ON DELETE CASCADE,
    last_page_id   BIGINT NOT NULL REFERENCES cutton_pages (page_id),
    progress_rate  DECIMAL(5, 2),
    created_at     TIMESTAMP NOT NULL,
    last_read_at   TIMESTAMP,
    completed_at   TIMESTAMP,
    quiz_passed_at TIMESTAMP,
    CONSTRAINT uk_reading_records_user_book UNIQUE (user_id, book_id)
);

CREATE INDEX idx_reading_records_book_id ON reading_records (book_id);

CREATE TABLE book_reviews (
    review_id   BIGSERIAL PRIMARY KEY,
    title       VARCHAR(100),
    content     TEXT,
    written_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    status      VARCHAR(20),
    record_id   BIGINT NOT NULL REFERENCES reading_records (record_id) ON DELETE CASCADE,
    report_date DATE,
    visibility  VARCHAR(10) NOT NULL DEFAULT 'PUBLIC'
);

CREATE INDEX idx_book_reviews_record_id ON book_reviews (record_id);

CREATE TABLE book_review_likes (
    review_like_id BIGSERIAL PRIMARY KEY,
    review_id      BIGINT NOT NULL REFERENCES book_reviews (review_id) ON DELETE CASCADE,
    user_id        BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    created_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_book_review_likes_review_user UNIQUE (review_id, user_id)
);

CREATE TABLE recommended_books (
    recommendation_id BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    book_id           BIGINT NOT NULL REFERENCES books (book_id) ON DELETE CASCADE,
    CONSTRAINT uk_recommended_books_user_book UNIQUE (user_id, book_id)
);

CREATE TABLE vocabularies (
    vocabulary_id BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    sentence_id   BIGINT NOT NULL REFERENCES sentences (sentence_id) ON DELETE CASCADE,
    word          VARCHAR(100) NOT NULL,
    pos           VARCHAR(20),
    target_code   VARCHAR(20) NOT NULL,
    choseong      VARCHAR(1) NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    CONSTRAINT uk_vocabularies_user_target UNIQUE (user_id, target_code)
);

CREATE INDEX idx_vocabularies_user_choseong ON vocabularies (user_id, choseong, created_at DESC);
CREATE INDEX idx_vocabularies_sentence_id ON vocabularies (sentence_id);

CREATE TABLE voca_senses (
    voca_sense_id BIGSERIAL PRIMARY KEY,
    vocabulary_id BIGINT NOT NULL REFERENCES vocabularies (vocabulary_id) ON DELETE CASCADE,
    sense_order   INTEGER NOT NULL,
    definition    TEXT NOT NULL,
    examples      TEXT
);

CREATE INDEX idx_voca_senses_vocabulary_id ON voca_senses (vocabulary_id);

CREATE TABLE highlights (
    highlight_id BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    sentence_id  BIGINT NOT NULL REFERENCES sentences (sentence_id) ON DELETE CASCADE,
    color        VARCHAR(20) NOT NULL,
    content      TEXT,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    memo         TEXT,
    CONSTRAINT uk_highlights_user_sentence UNIQUE (user_id, sentence_id)
);

CREATE INDEX idx_highlights_sentence_id ON highlights (sentence_id);

CREATE TABLE book_quizzes (
    quiz_id        BIGSERIAL PRIMARY KEY,
    book_id        BIGINT NOT NULL REFERENCES books (book_id) ON DELETE CASCADE,
    question_order INTEGER NOT NULL,
    question       TEXT NOT NULL,
    choice1        VARCHAR(255) NOT NULL,
    choice2        VARCHAR(255) NOT NULL,
    choice3        VARCHAR(255) NOT NULL,
    choice4        VARCHAR(255) NOT NULL,
    correct_index  INTEGER NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_book_quizzes_book_order UNIQUE (book_id, question_order)
);

CREATE INDEX idx_book_quizzes_book_id ON book_quizzes (book_id);

CREATE TABLE book_status (
    book_status_id   BIGSERIAL PRIMARY KEY,
    book_id          BIGINT NOT NULL REFERENCES books (book_id) ON DELETE CASCADE,
    user_id          BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    count            BIGINT,
    last_page_id     BIGINT REFERENCES cutton_pages (page_id),
    progress_percent DECIMAL(5, 2),
    last_read_at     TIMESTAMP,
    CONSTRAINT uk_book_status_user_book UNIQUE (user_id, book_id)
);

CREATE INDEX idx_book_status_book_id ON book_status (book_id);

CREATE TABLE training_rooms (
    training_room_id BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    book_review_id   BIGINT NOT NULL REFERENCES book_reviews (review_id) ON DELETE CASCADE,
    created_at       TIMESTAMP NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
);

CREATE INDEX idx_training_rooms_user_id ON training_rooms (user_id);
CREATE INDEX idx_training_rooms_book_review_id ON training_rooms (book_review_id);
CREATE UNIQUE INDEX uk_training_rooms_user_in_progress
    ON training_rooms (user_id)
    WHERE status <> 'COMPLETED';

CREATE TABLE training_chats (
    training_chat_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    training_room_id BIGINT NOT NULL REFERENCES training_rooms (training_room_id) ON DELETE CASCADE,
    role             VARCHAR(10) NOT NULL,
    message_type     VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    content          TEXT,
    created_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_training_chats_training_room_id ON training_chats (training_room_id);

CREATE TABLE training_summations (
    training_summation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    training_room_id      BIGINT NOT NULL UNIQUE REFERENCES training_rooms (training_room_id) ON DELETE CASCADE,
    subject               TEXT,
    upgrade_point         TEXT,
    first_think           TEXT,
    final_think           TEXT
);

CREATE TABLE understand_rooms (
    understand_room_id BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    book_id            BIGINT NOT NULL REFERENCES books (book_id) ON DELETE CASCADE,
    created_at         TIMESTAMP NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    start_page_number  INTEGER,
    end_page_number    INTEGER
);

CREATE INDEX idx_understand_rooms_user_id ON understand_rooms (user_id);
CREATE INDEX idx_understand_rooms_book_id ON understand_rooms (book_id);
CREATE UNIQUE INDEX uk_understand_rooms_user_in_progress
    ON understand_rooms (user_id)
    WHERE status <> 'COMPLETED';

CREATE TABLE understand_chats (
    understand_chat_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    understand_room_id BIGINT NOT NULL REFERENCES understand_rooms (understand_room_id) ON DELETE CASCADE,
    role               VARCHAR(10) NOT NULL,
    message_type       VARCHAR(10) NOT NULL DEFAULT 'TEXT',
    content            TEXT,
    created_at         TIMESTAMP NOT NULL
);

CREATE INDEX idx_understand_chats_understand_room_id ON understand_chats (understand_room_id);

CREATE TABLE understand_summations (
    understand_summation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    understand_room_id      BIGINT NOT NULL UNIQUE REFERENCES understand_rooms (understand_room_id) ON DELETE CASCADE,
    subject                 TEXT,
    result                  TEXT
);

CREATE TABLE review_analyze (
    review_analyze_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    training_room_id      BIGINT REFERENCES training_rooms (training_room_id) ON DELETE CASCADE,
    book_review_id        BIGINT NOT NULL REFERENCES book_reviews (review_id) ON DELETE CASCADE,
    access_level          VARCHAR(10),
    access_note           TEXT,
    understand_level      VARCHAR(10),
    understand_note       TEXT,
    inference_level       VARCHAR(10),
    inference_note        TEXT,
    evaluate_level        VARCHAR(10),
    evaluate_note         TEXT,
    critical_level        VARCHAR(10),
    critical_note         TEXT,
    created_at            TIMESTAMP NOT NULL,
    ambiguous             BOOLEAN NOT NULL DEFAULT FALSE,
    perspective_summary   TEXT,
    perspective_embedding vector(768)
);

CREATE INDEX idx_review_analyze_training_room_id ON review_analyze (training_room_id);
CREATE INDEX idx_review_analyze_book_review_id ON review_analyze (book_review_id);

CREATE TABLE review_analyze_verification (
    verification_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_analyze_id BIGINT NOT NULL REFERENCES review_analyze (review_analyze_id) ON DELETE CASCADE,
    triggered_reason  VARCHAR(30) NOT NULL,
    corrected         BOOLEAN NOT NULL,
    corrected_domains TEXT,
    verifier_note     TEXT,
    created_at        TIMESTAMP NOT NULL
);

CREATE INDEX idx_review_analyze_verification_review_analyze_id
    ON review_analyze_verification (review_analyze_id);

CREATE TABLE user_literacy_profiles (
    user_id          BIGINT PRIMARY KEY REFERENCES users (user_id) ON DELETE CASCADE,
    access_score     DOUBLE PRECISION NOT NULL,
    understand_score DOUBLE PRECISION NOT NULL,
    inference_score  DOUBLE PRECISION NOT NULL,
    evaluate_score   DOUBLE PRECISION NOT NULL,
    critical_score   DOUBLE PRECISION NOT NULL,
    session_count    INTEGER NOT NULL DEFAULT 1,
    updated_at       TIMESTAMP NOT NULL
);

CREATE TABLE training_coaching_signals (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    training_room_id     BIGINT NOT NULL UNIQUE REFERENCES training_rooms (training_room_id) ON DELETE CASCADE,
    user_id              BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    target_domains       VARCHAR(100) NOT NULL,
    score_before         DOUBLE PRECISION NOT NULL,
    score_after          DOUBLE PRECISION,
    delta                DOUBLE PRECISION,
    status               VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    conversation_excerpt TEXT,
    embedding            vector(768),
    version              INTEGER NOT NULL DEFAULT 1,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL,
    resolved_at          TIMESTAMP,
    reviewed_at          TIMESTAMP,
    reviewed_by          BIGINT REFERENCES users (user_id)
);

CREATE INDEX idx_training_coaching_signals_user_status ON training_coaching_signals (user_id, status);

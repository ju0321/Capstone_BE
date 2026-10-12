package com.capstone.be.domain.book.enums;

/**
 * 책 장르/카테고리 대분류. genres 테이블의 type 컬럼에 이 enum의 name() 값이 그대로 저장된다.
 * "어린이·청소년", "수험서·교육"은 제외한 고정 목록이다 - V17__create_genres_table.sql 참고.
 * Java 식별자 규칙상 "/", "·" 같은 기호는 쓸 수 없어서, 두 단어로 된 카테고리는 밑줄(_)로 이어붙였다
 * (예: 경제/경영 -> 경제_경영).
 */
public enum GenreType {
    소설,
    인문,
    경제_경영,
    자기계발,
    사회_정치,
    과학,
    예술_대중문화,
    컴퓨터_IT,
    여행_취미,
    에세이,
    시_희곡,
}

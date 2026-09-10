package kr.co.seoulit.his.receptionservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;

/**
 * 접수(Reception) 도메인 Repository
 */
public interface ReceptionRepository extends JpaRepository<ReceptionEntity, String> {

    /**
     * 접수 목록 조회 (접수일시 최신순, 특정 접수유형 제외).
     * 접수홈 목록은 응급접수를 제외하고 보여준다 (응급은 응급접수홈 전용 목록으로).
     */
    List<ReceptionEntity> findByReceptionTypeNotOrderByReceptionDateDesc(String receptionType);

    /**
     * 특정 접수유형의 당일 접수 목록 조회.
     * 외래 접수 이벤트를 놓쳤을 때 재발행(보정)하는 용도.
     * receptionDate 가 LocalDateTime 이므로 [당일 00:00, 익일 00:00) 범위로 조회한다.
     */
    List<ReceptionEntity> findByReceptionTypeAndReceptionDateBetween(
            String receptionType, LocalDateTime startInclusive, LocalDateTime endExclusive);

}

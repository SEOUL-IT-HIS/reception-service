package kr.co.seoulit.his.receptionservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;

/**
 * 접수(Reception) 도메인 Repository
 */
public interface ReceptionRepository extends JpaRepository<ReceptionEntity, Long> {

    /** 접수 목록 조회 (접수일시 최신순) */
    List<ReceptionEntity> findAllByOrderByReceptionDateDesc();

}

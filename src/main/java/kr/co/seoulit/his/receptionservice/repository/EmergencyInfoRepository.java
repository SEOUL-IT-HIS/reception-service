package kr.co.seoulit.his.receptionservice.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.seoulit.his.receptionservice.entity.EmergencyInfoEntity;

/**
 * 응급접수정보(EmergencyInfo) 도메인 Repository
 */
public interface EmergencyInfoRepository extends JpaRepository<EmergencyInfoEntity, String> {

    /** 접수ID 목록으로 응급접수정보 일괄 조회 (응급접수 목록 조회용) */
    List<EmergencyInfoEntity> findByReceptionIdIn(Collection<String> receptionIds);

}

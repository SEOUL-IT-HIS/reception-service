package kr.co.seoulit.his.receptionservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.seoulit.his.receptionservice.entity.EmergencyInfoEntity;

/**
 * 응급접수정보(EmergencyInfo) 도메인 Repository
 */
public interface EmergencyInfoRepository extends JpaRepository<EmergencyInfoEntity, Long> {

}

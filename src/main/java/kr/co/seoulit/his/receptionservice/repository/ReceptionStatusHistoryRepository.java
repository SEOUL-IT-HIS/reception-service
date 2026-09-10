package kr.co.seoulit.his.receptionservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.seoulit.his.receptionservice.entity.ReceptionStatusHistoryEntity;

/**
 * 접수상태변경이력(ReceptionStatusHistory) 도메인 Repository
 */
public interface ReceptionStatusHistoryRepository extends JpaRepository<ReceptionStatusHistoryEntity, String> {

}

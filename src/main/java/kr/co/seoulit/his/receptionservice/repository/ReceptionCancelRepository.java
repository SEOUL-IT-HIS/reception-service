package kr.co.seoulit.his.receptionservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.seoulit.his.receptionservice.entity.ReceptionCancelEntity;

/**
 * 접수취소(ReceptionCancel) 도메인 Repository
 */
public interface ReceptionCancelRepository extends JpaRepository<ReceptionCancelEntity, String> {

}

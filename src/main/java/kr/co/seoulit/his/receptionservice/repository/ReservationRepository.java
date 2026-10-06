package kr.co.seoulit.his.receptionservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kr.co.seoulit.his.receptionservice.entity.ReservationEntity;

/**
 * 예약(Reservation) 도메인 Repository
 */
public interface ReservationRepository extends JpaRepository<ReservationEntity, String> {

    /** 예약일이 기준일 이후인 예약 (예약 목록용) — 최근에 등록한 예약이 먼저 */
    List<ReservationEntity> findByReservationDateGreaterThanEqualOrderByCreatedAtDesc(
            LocalDate from);

    /** 접수하기 시 같은 예약으로 접수가 두 번 만들어지지 않도록 행을 잠그고 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservationEntity r where r.reservationId = :reservationId")
    Optional<ReservationEntity> findByIdForUpdate(@Param("reservationId") String reservationId);
}

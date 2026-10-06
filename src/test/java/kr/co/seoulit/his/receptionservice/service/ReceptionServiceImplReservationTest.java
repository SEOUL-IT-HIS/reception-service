package kr.co.seoulit.his.receptionservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import kr.co.seoulit.his.receptionservice.dto.request.ReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReservationRequestdto;
import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;
import kr.co.seoulit.his.receptionservice.entity.ReservationEntity;
import kr.co.seoulit.his.receptionservice.exception.InvalidReservationException;
import kr.co.seoulit.his.receptionservice.exception.ReservationNotFoundException;
import kr.co.seoulit.his.receptionservice.exception.ReservationNotReceivableException;
import kr.co.seoulit.his.receptionservice.repository.ReceptionRepository;
import kr.co.seoulit.his.receptionservice.repository.ReservationRepository;

@ExtendWith(MockitoExtension.class)
class ReceptionServiceImplReservationTest {

    @Mock
    private ReceptionRepository receptionRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReceptionServiceImpl service;

    private ReceptionRequestdto request(String reservationId) {
        return ReceptionRequestdto.builder()
                .patientId("p1")
                .deptId("01")
                .doctorId("emp-1")
                .visitType("REVISIT")
                .receptionType("RESERVATION") // 클라이언트가 보내도 무시되어야 한다
                .reservationId(reservationId)
                .memo("메모")
                .build();
    }

    private ReservationEntity reservation(String status, String patientId) {
        return ReservationEntity.builder()
                .reservationId("rv-1")
                .patientId(patientId)
                .deptId("01")
                .doctorId("emp-1")
                .visitType("REVISIT")
                .reservationDate(LocalDate.now())
                .reservationTime("14:30")
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void 예약ID_없이_직접_입력한_접수는_당일로_저장된다() {
        service.registerReception(request(null));

        ArgumentCaptor<ReceptionEntity> saved = ArgumentCaptor.forClass(ReceptionEntity.class);
        verify(receptionRepository).save(saved.capture());
        assertThat(saved.getValue().getReceptionType()).isEqualTo("WALK_IN");
        verify(reservationRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void 예약에서_접수하면_예약유형으로_저장되고_예약이_접수완료가_된다() {
        ReservationEntity reservation = reservation(ReservationEntity.STATUS_RESERVED, "p1");
        when(reservationRepository.findByIdForUpdate("rv-1")).thenReturn(Optional.of(reservation));

        service.registerReception(request("rv-1"));

        ArgumentCaptor<ReceptionEntity> saved = ArgumentCaptor.forClass(ReceptionEntity.class);
        verify(receptionRepository).save(saved.capture());
        assertThat(saved.getValue().getReceptionType()).isEqualTo("RESERVATION");
        assertThat(reservation.getStatus()).isEqualTo(ReservationEntity.STATUS_RECEIVED);
        assertThat(reservation.getReceptionId()).isEqualTo(saved.getValue().getReceptionId());
    }

    @Test
    void 이미_접수된_예약으로는_접수할_수_없다() {
        when(reservationRepository.findByIdForUpdate("rv-1"))
                .thenReturn(Optional.of(reservation(ReservationEntity.STATUS_RECEIVED, "p1")));

        assertThatThrownBy(() -> service.registerReception(request("rv-1")))
                .isInstanceOf(ReservationNotReceivableException.class);
        verify(receptionRepository, never()).save(any());
    }

    @Test
    void 예약의_환자와_다른_환자로는_접수할_수_없다() {
        when(reservationRepository.findByIdForUpdate("rv-1"))
                .thenReturn(Optional.of(reservation(ReservationEntity.STATUS_RESERVED, "other")));

        assertThatThrownBy(() -> service.registerReception(request("rv-1")))
                .isInstanceOf(ReservationNotReceivableException.class);
        verify(receptionRepository, never()).save(any());
    }

    @Test
    void 없는_예약ID면_예외() {
        when(reservationRepository.findByIdForUpdate("rv-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerReception(request("rv-1")))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void 예약_등록은_지난_날짜와_잘못된_시간을_거부한다() {
        ReservationRequestdto past = ReservationRequestdto.builder()
                .patientId("p1").deptId("01").doctorId("emp-1").visitType("INITIAL")
                .reservationDate(LocalDate.now().minusDays(1)).reservationTime("10:00").build();
        assertThatThrownBy(() -> service.registerReservation(past)).isInstanceOf(InvalidReservationException.class);

        ReservationRequestdto badTime = ReservationRequestdto.builder()
                .patientId("p1").deptId("01").doctorId("emp-1").visitType("INITIAL")
                .reservationDate(LocalDate.now()).reservationTime("25:00").build();
        assertThatThrownBy(() -> service.registerReservation(badTime)).isInstanceOf(InvalidReservationException.class);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 예약_등록은_예약됨_상태로_저장한다() {
        service.registerReservation(ReservationRequestdto.builder()
                .patientId("p1").deptId("01").doctorId("emp-1").visitType("INITIAL")
                .reservationDate(LocalDate.now()).reservationTime("09:05").memo("검진").build());

        ArgumentCaptor<ReservationEntity> saved = ArgumentCaptor.forClass(ReservationEntity.class);
        verify(reservationRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ReservationEntity.STATUS_RESERVED);
        assertThat(saved.getValue().getReceptionId()).isNull();
        assertThat(saved.getValue().getReservationTime()).isEqualTo("09:05");
    }

    @Test
    void 예약됨_상태의_예약은_취소할_수_있다() {
        ReservationEntity reservation = reservation(ReservationEntity.STATUS_RESERVED, "p1");
        when(reservationRepository.findByIdForUpdate("rv-1")).thenReturn(Optional.of(reservation));

        service.cancelReservation("rv-1");

        assertThat(reservation.getStatus()).isEqualTo(ReservationEntity.STATUS_CANCELLED);
    }

    @Test
    void 이미_접수되었거나_취소된_예약은_취소할_수_없다() {
        for (String status : new String[] {ReservationEntity.STATUS_RECEIVED, ReservationEntity.STATUS_CANCELLED}) {
            ReservationEntity reservation = reservation(status, "p1");
            when(reservationRepository.findByIdForUpdate("rv-1")).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.cancelReservation("rv-1"))
                    .isInstanceOf(ReservationNotReceivableException.class);
            assertThat(reservation.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void 취소된_예약으로는_접수할_수_없다() {
        when(reservationRepository.findByIdForUpdate("rv-1"))
                .thenReturn(Optional.of(reservation(ReservationEntity.STATUS_CANCELLED, "p1")));

        assertThatThrownBy(() -> service.registerReception(request("rv-1")))
                .isInstanceOf(ReservationNotReceivableException.class);
        verify(receptionRepository, never()).save(any());
    }
}

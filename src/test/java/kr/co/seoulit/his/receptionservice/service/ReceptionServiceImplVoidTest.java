package kr.co.seoulit.his.receptionservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import kr.co.seoulit.his.receptionservice.delegate.EmergencyCancelCheckBusinessDelegate;
import kr.co.seoulit.his.receptionservice.entity.ReceptionCancelEntity;
import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;
import kr.co.seoulit.his.receptionservice.messaging.event.EmergencyReceptionCancelledInternalEvent;
import kr.co.seoulit.his.receptionservice.repository.EmergencyInfoRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionCancelRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionStatusHistoryRepository;

@ExtendWith(MockitoExtension.class)
class ReceptionServiceImplVoidTest {

    @Mock
    private ReceptionRepository receptionRepository;
    @Mock
    private EmergencyInfoRepository emergencyInfoRepository;
    @Mock
    private ReceptionCancelRepository receptionCancelRepository;
    @Mock
    private ReceptionStatusHistoryRepository receptionStatusHistoryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private EmergencyCancelCheckBusinessDelegate emergencyCancelCheck;

    @InjectMocks
    private ReceptionServiceImpl service;

    private ReceptionEntity reception(String type, String status) {
        ReceptionEntity r = ReceptionEntity.builder()
                .receptionId("er-1").patientId("p1").deptId("01").doctorId("emp-1")
                .receptionType(type).status(status)
                .receptionDate(LocalDateTime.now()).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(receptionRepository.findById("er-1")).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void 응급_기록이_있어도_오접수로_취소된다_응급_취소가능여부는_묻지_않는다() {
        ReceptionEntity r = reception("EMERGENCY", "RECEPTION");
        // 일반 취소라면 막혔을 상황 — 조회 빈이 있어도 이 경로는 부르지 않아야 한다.
        ReflectionTestUtils.setField(service, "emergencyCancelCheckBusinessDelegate", Optional.of(emergencyCancelCheck));

        service.voidEmergencyReception("er-1", "ERRONEOUS", "환자를 잘못 선택", null, "emp-9");

        assertThat(r.getStatus()).isEqualTo("CANCELLED");
        verifyNoInteractions(emergencyCancelCheck);
        ArgumentCaptor<ReceptionCancelEntity> cancel = ArgumentCaptor.forClass(ReceptionCancelEntity.class);
        verify(receptionCancelRepository).save(cancel.capture());
        assertThat(cancel.getValue().getCancelReasonCode()).isEqualTo("MISTAKE");
        assertThat(cancel.getValue().getCancelReasonDetail()).isEqualTo("환자를 잘못 선택");
        assertThat(cancel.getValue().getCancelledBy()).isEqualTo("emp-9");
        // 취소 후 응급으로 ReceptionCancelled 가 나가도록 예약된다
        verify(eventPublisher).publishEvent(any(EmergencyReceptionCancelledInternalEvent.class));
    }

    @Test
    void 중복_접수는_DUPLICATE_코드로_저장되고_남기는_접수가_사유에_붙는다() {
        reception("EMERGENCY", "RECEPTION");

        service.voidEmergencyReception("er-1", "DUPLICATE", "같은 환자 중복", "er-keep", "emp-9");

        ArgumentCaptor<ReceptionCancelEntity> cancel = ArgumentCaptor.forClass(ReceptionCancelEntity.class);
        verify(receptionCancelRepository).save(cancel.capture());
        assertThat(cancel.getValue().getCancelReasonCode()).isEqualTo("DUPLICATE");
        assertThat(cancel.getValue().getCancelReasonDetail()).isEqualTo("같은 환자 중복 (유지 접수: er-keep)");
    }

    @Test
    void 알_수_없는_사유_유형은_ETC_이고_결정자가_없으면_emergency_service() {
        reception("EMERGENCY", "RECEPTION");

        service.voidEmergencyReception("er-1", "WHATEVER", null, null, " ");

        ArgumentCaptor<ReceptionCancelEntity> cancel = ArgumentCaptor.forClass(ReceptionCancelEntity.class);
        verify(receptionCancelRepository).save(cancel.capture());
        assertThat(cancel.getValue().getCancelReasonCode()).isEqualTo("ETC");
        assertThat(cancel.getValue().getCancelReasonDetail()).isNull();
        assertThat(cancel.getValue().getCancelledBy()).isEqualTo("emergency-service");
    }

    @Test
    void 사유_상세와_취소자는_컬럼_길이에_맞춰_자른다() {
        reception("EMERGENCY", "RECEPTION");

        service.voidEmergencyReception("er-1", "ERRONEOUS", "가".repeat(700), null, "x".repeat(80));

        ArgumentCaptor<ReceptionCancelEntity> cancel = ArgumentCaptor.forClass(ReceptionCancelEntity.class);
        verify(receptionCancelRepository).save(cancel.capture());
        assertThat(cancel.getValue().getCancelReasonDetail()).hasSize(500);
        assertThat(cancel.getValue().getCancelledBy()).hasSize(50);
    }

    @Test
    void 이미_취소된_접수는_같은_이벤트가_다시_와도_그대로_둔다() {
        reception("EMERGENCY", "CANCELLED");

        service.voidEmergencyReception("er-1", "DUPLICATE", "재전송", null, "emp-9");

        verify(receptionCancelRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 응급_접수가_아니면_무시한다() {
        ReceptionEntity r = reception("WALK_IN", "RECEPTION");

        service.voidEmergencyReception("er-1", "DUPLICATE", "잘못 온 이벤트", null, "emp-9");

        assertThat(r.getStatus()).isEqualTo("RECEPTION");
        verify(receptionCancelRepository, never()).save(any());
    }

    @Test
    void 없는_접수는_무시한다() {
        when(receptionRepository.findById("none")).thenReturn(Optional.empty());

        service.voidEmergencyReception("none", "DUPLICATE", "없음", null, "emp-9");

        verify(receptionCancelRepository, never()).save(any());
    }
}

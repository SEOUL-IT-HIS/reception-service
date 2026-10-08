package kr.co.seoulit.his.receptionservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import kr.co.seoulit.his.receptionservice.delegate.OutpatientCancelCheckBusinessDelegate;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionCancelRequestdto;
import kr.co.seoulit.his.receptionservice.dto.response.OutpatientEncounterStatusResponsedto;
import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;
import kr.co.seoulit.his.receptionservice.exception.OutpatientCancelCheckFailedException;
import kr.co.seoulit.his.receptionservice.exception.OutpatientCancelNotAllowedException;
import kr.co.seoulit.his.receptionservice.repository.ReceptionCancelRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionStatusHistoryRepository;

@ExtendWith(MockitoExtension.class)
class ReceptionServiceImplOutpatientCancelTest {

    @Mock
    private ReceptionRepository receptionRepository;
    @Mock
    private ReceptionCancelRepository receptionCancelRepository;
    @Mock
    private ReceptionStatusHistoryRepository receptionStatusHistoryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private OutpatientCancelCheckBusinessDelegate delegate;

    @InjectMocks
    private ReceptionServiceImpl service;

    private ReceptionEntity reception;

    private void givenOutpatientReception() {
        reception = ReceptionEntity.builder()
                .receptionId("r1").patientId("p1").deptId("01").doctorId("emp-1")
                .receptionType("WALK_IN").visitType("REVISIT").status("RECEPTION")
                .receptionDate(LocalDateTime.now()).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(receptionRepository.findById("r1")).thenReturn(Optional.of(reception));
    }

    private void enableCheck() {
        ReflectionTestUtils.setField(service, "outpatientCancelCheckBusinessDelegate", Optional.of(delegate));
    }

    private void disableCheck() {
        ReflectionTestUtils.setField(service, "outpatientCancelCheckBusinessDelegate", Optional.empty());
    }

    private ReceptionCancelRequestdto request() {
        return ReceptionCancelRequestdto.builder().cancelReasonCode("PATIENT_REQUEST").cancelledBy("admin").build();
    }

    private OutpatientEncounterStatusResponsedto status(String status, boolean inProgress) {
        return new OutpatientEncounterStatusResponsedto("r1", "e1", status, inProgress);
    }

    @Test
    void 진료_중인_접수는_취소할_수_없다() {
        givenOutpatientReception();
        enableCheck();
        when(delegate.getStatus("r1")).thenReturn(Optional.of(status("IN_PROGRESS", true)));

        assertThatThrownBy(() -> service.cancelReception("r1", request()))
                .isInstanceOf(OutpatientCancelNotAllowedException.class);
        assertThat(reception.getStatus()).isEqualTo("RECEPTION");
        verify(receptionCancelRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 진료가_끝난_접수도_취소할_수_없다() {
        givenOutpatientReception();
        enableCheck();
        when(delegate.getStatus("r1")).thenReturn(Optional.of(status("COMPLETED", false)));

        assertThatThrownBy(() -> service.cancelReception("r1", request()))
                .isInstanceOf(OutpatientCancelNotAllowedException.class);
        assertThat(reception.getStatus()).isEqualTo("RECEPTION");
    }

    @Test
    void 대기_중인_접수는_취소된다() {
        givenOutpatientReception();
        enableCheck();
        when(delegate.getStatus("r1")).thenReturn(Optional.of(status("WAITING", false)));

        service.cancelReception("r1", request());

        assertThat(reception.getStatus()).isEqualTo("CANCELLED");
        verify(receptionCancelRepository).save(any());
    }

    @Test
    void 외래에_등록되지_않은_접수는_404로_취소된다() {
        givenOutpatientReception();
        enableCheck();
        when(delegate.getStatus("r1")).thenReturn(Optional.empty());

        service.cancelReception("r1", request());

        assertThat(reception.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void 외래_조회가_실패하면_취소를_막는다() {
        givenOutpatientReception();
        enableCheck();
        when(delegate.getStatus("r1")).thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> service.cancelReception("r1", request()))
                .isInstanceOf(OutpatientCancelCheckFailedException.class);
        assertThat(reception.getStatus()).isEqualTo("RECEPTION");
        verify(receptionCancelRepository, never()).save(any());
    }

    @Test
    void 기능이_꺼져_있으면_확인_없이_취소된다() {
        givenOutpatientReception();
        disableCheck();

        service.cancelReception("r1", request());

        assertThat(reception.getStatus()).isEqualTo("CANCELLED");
    }
}

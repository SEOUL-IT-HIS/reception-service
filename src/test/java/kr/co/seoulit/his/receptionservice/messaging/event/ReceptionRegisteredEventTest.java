package kr.co.seoulit.his.receptionservice.messaging.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;

class ReceptionRegisteredEventTest {

    @Test
    void 엔티티_스냅샷을_외래_전달_이벤트로_매핑한다() {
        ReceptionEntity reception = ReceptionEntity.builder()
                .receptionId("10537")
                .patientId("b1e6c9d0-2f3a-4c5b-8d7e-9a0b1c2d3e4f")
                .deptId("10")
                .doctorId("20")
                .receptionType("외래")
                .status("RECEPTION")
                .memo("정기 진료")
                .receptionDate(LocalDateTime.of(2026, 9, 3, 10, 15, 32))
                .build();

        ReceptionRegisteredInternalEvent snapshot = ReceptionRegisteredInternalEvent.from(reception);
        ReceptionRegisteredEvent event = ReceptionRegisteredEvent.from(snapshot);

        assertThat(event.eventId()).isNotBlank();
        assertThat(event.eventType()).isEqualTo("ReceptionRegistered");
        assertThat(event.version()).isEqualTo("1.0");
        assertThat(event.source()).isEqualTo("RCP");
        assertThat(event.occurredAt().toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 9, 3, 10, 15, 32));

        ReceptionRegisteredEvent.ReceptionData data = event.data();
        assertThat(data.receptionId()).isEqualTo("10537");
        assertThat(data.patientId()).isEqualTo("b1e6c9d0-2f3a-4c5b-8d7e-9a0b1c2d3e4f");
        assertThat(data.departmentCode()).isEqualTo("10");
        assertThat(data.doctorId()).isEqualTo("20");
        assertThat(data.visitDate()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(data.status()).isEqualTo("RECEPTION");
        assertThat(data.visitReason()).isEqualTo("정기 진료");
    }

    @Test
    void deptId가_null이면_departmentCode도_null이다() {
        ReceptionEntity reception = ReceptionEntity.builder()
                .receptionId("1")
                .patientId("p1")
                .receptionType("외래")
                .status("RECEPTION")
                .receptionDate(LocalDateTime.now())
                .build();

        ReceptionRegisteredEvent event =
                ReceptionRegisteredEvent.from(ReceptionRegisteredInternalEvent.from(reception));

        assertThat(event.data().departmentCode()).isNull();
    }
}

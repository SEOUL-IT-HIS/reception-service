package kr.co.seoulit.his.receptionservice.messaging.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import kr.co.seoulit.his.receptionservice.entity.EmergencyInfoEntity;
import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;

class ReceptionRegisteredEventTest {

    @Test
    void 엔티티_스냅샷을_외래_전달_이벤트로_매핑한다() {
        ReceptionEntity reception = ReceptionEntity.builder()
                .receptionId("10537")
                .patientId("b1e6c9d0-2f3a-4c5b-8d7e-9a0b1c2d3e4f")
                .deptId("10")
                .doctorId("20")
                .receptionType("WALK_IN")
                .visitType("REVISIT")
                .status("RECEPTION")
                .memo("정기 진료")
                .receptionDate(LocalDateTime.of(2026, 9, 3, 10, 15, 32))
                .build();

        ReceptionRegisteredInternalEvent snapshot = ReceptionRegisteredInternalEvent.from(reception);
        ReceptionRegisteredEvent event = ReceptionRegisteredEvent.from(snapshot);

        assertThat(event.eventId()).isNotBlank();
        assertThat(event.eventType()).isEqualTo("ReceptionRegistered");
        assertThat(event.version()).isEqualTo("1.1");
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
        assertThat(data.visitType()).isEqualTo("REVISIT");
        assertThat(data.receptionType()).isEqualTo("WALK_IN");
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

    @Test
    void 취소_이벤트는_등록과_같은_값에_status만_CANCELLED로_보낸다() {
        ReceptionEntity reception = ReceptionEntity.builder()
                .receptionId("10537")
                .patientId("p1")
                .deptId("01")
                .doctorId("emp-1")
                .receptionType("RESERVATION")
                .visitType("INITIAL")
                .status("RECEPTION")
                .memo("정기 진료")
                .receptionDate(LocalDateTime.of(2026, 9, 3, 10, 15, 32))
                .build();
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 9, 3, 11, 0, 0);
        reception.changeStatus("CANCELLED", cancelledAt);

        ReceptionCancelledInternalEvent cancelled = ReceptionCancelledInternalEvent.from(reception, cancelledAt);
        ReceptionRegisteredEvent event = ReceptionRegisteredEvent.cancelled(
                cancelled.snapshot(), cancelledAt.atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());

        assertThat(event.eventType()).isEqualTo("ReceptionCancelled");
        assertThat(event.occurredAt().toLocalDateTime()).isEqualTo(cancelledAt);

        ReceptionRegisteredEvent.ReceptionData data = event.data();
        assertThat(data.receptionId()).isEqualTo("10537");
        assertThat(data.status()).isEqualTo("CANCELLED");
        assertThat(data.departmentCode()).isEqualTo("01");
        assertThat(data.doctorId()).isEqualTo("emp-1");
        assertThat(data.visitDate()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(data.visitReason()).isEqualTo("정기 진료");
        assertThat(data.visitType()).isEqualTo("INITIAL");
        assertThat(data.receptionType()).isEqualTo("RESERVATION");
    }

    @Test
    void 응급_취소_이벤트는_등록_필드에_eventType과_status를_붙인다() {
        EmergencyReceptionRegisteredInternalEvent snapshot = new EmergencyReceptionRegisteredInternalEvent(
                "er-1", "p1", "01", LocalDateTime.of(2026, 9, 3, 10, 0), "메모", "흉통",
                2, LocalDateTime.of(2026, 9, 3, 10, 0));
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 9, 3, 10, 30);

        ReceptionIntakeCancelledEvent event =
                ReceptionIntakeCancelledEvent.from(new EmergencyReceptionCancelledInternalEvent(snapshot, cancelledAt));

        assertThat(event.receptionId()).isEqualTo("er-1");
        assertThat(event.arrivalPath()).isEqualTo("01");
        assertThat(event.receivedAt()).isEqualTo(LocalDateTime.of(2026, 9, 3, 10, 0));
        assertThat(event.chiefComplaintRaw()).isEqualTo("흉통");
        assertThat(event.ktasLevel()).isEqualTo(2);
        assertThat(event.occurredAt()).isEqualTo(cancelledAt);
        assertThat(event.eventType()).isEqualTo("ReceptionCancelled");
        assertThat(event.status()).isEqualTo("CANCELLED");
    }

    @Test
    void 응급_등록_이벤트에_KTAS를_담는다() {
        ReceptionEntity reception = ReceptionEntity.builder()
                .receptionId("er-1")
                .patientId("p1")
                .receptionType("EMERGENCY")
                .status("RECEPTION")
                .memo("메모")
                .receptionDate(LocalDateTime.of(2026, 9, 3, 10, 0))
                .build();
        EmergencyInfoEntity info = EmergencyInfoEntity.builder()
                .receptionId("er-1")
                .ktasLevel(3)
                .visitMethod("02")
                .chiefComplaint("복통")
                .triageDateTime(LocalDateTime.of(2026, 9, 3, 10, 0, 5))
                .build();

        ReceptionIntakeEvent event =
                ReceptionIntakeEvent.from(EmergencyReceptionRegisteredInternalEvent.from(reception, info));

        assertThat(event.receptionId()).isEqualTo("er-1");
        assertThat(event.arrivalPath()).isEqualTo("02");
        assertThat(event.chiefComplaintRaw()).isEqualTo("복통");
        assertThat(event.ktasLevel()).isEqualTo(3);
        assertThat(event.triageDateTime()).isEqualTo(LocalDateTime.of(2026, 9, 3, 10, 0, 5));
    }
}

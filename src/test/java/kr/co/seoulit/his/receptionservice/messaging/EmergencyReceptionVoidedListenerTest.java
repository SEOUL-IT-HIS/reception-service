package kr.co.seoulit.his.receptionservice.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.co.seoulit.his.receptionservice.service.ReceptionService;

@ExtendWith(MockitoExtension.class)
class EmergencyReceptionVoidedListenerTest {

    @Mock
    private ReceptionService receptionService;

    @InjectMocks
    private EmergencyReceptionVoidedListener listener;

    private static final String VALID = """
            {"eventId":"00000000-0000-0000-0000-000000000001","eventType":"ReceptionVoided","version":"1.0","source":"ER",
             "occurredAt":"2026-10-08T14:30:00+09:00","receptionId":"er-1","patientId":"p1",
             "reasonType":"DUPLICATE","reasonText":"중복","keptReceptionId":"er-2","decidedBy":"emp-9"}
            """;

    @Test
    void 합의한_규격의_이벤트를_읽어_서비스에_넘긴다() {
        listener.handle(VALID);

        verify(receptionService).voidEmergencyReception("er-1", "DUPLICATE", "중복", "er-2", "emp-9");
    }

    @Test
    void keptReceptionId_가_null_이거나_모르는_필드가_섞여도_읽는다() {
        listener.handle("""
                {"eventType":"ReceptionVoided","receptionId":"er-1","reasonType":"ERRONEOUS","reasonText":"오접수",
                 "keptReceptionId":null,"decidedBy":"emp-9","newFieldFromFuture":123}
                """);

        verify(receptionService).voidEmergencyReception("er-1", "ERRONEOUS", "오접수", null, "emp-9");
    }

    @Test
    void 다른_eventType_은_건너뛴다() {
        listener.handle("{\"eventType\":\"SomethingElse\",\"receptionId\":\"er-1\"}");

        verify(receptionService, never()).voidEmergencyReception(any(), any(), any(), any(), any());
    }

    @Test
    void receptionId_가_없으면_건너뛴다() {
        listener.handle("{\"eventType\":\"ReceptionVoided\",\"reasonType\":\"DUPLICATE\"}");

        verify(receptionService, never()).voidEmergencyReception(any(), any(), any(), any(), any());
    }

    @Test
    void 깨진_메시지는_예외를_던지지_않고_건너뛴다() {
        listener.handle("not json at all {");

        verify(receptionService, never()).voidEmergencyReception(any(), any(), any(), any(), any());
    }
}

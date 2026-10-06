package kr.co.seoulit.his.receptionservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import kr.co.seoulit.his.receptionservice.delegate.OutpatientVisitHistoryBusinessDelegate;
import kr.co.seoulit.his.receptionservice.dto.response.OutpatientVisitHistoryResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.VisitTypeResponsedto;

@ExtendWith(MockitoExtension.class)
class ReceptionServiceImplVisitTypeTest {

    @Mock
    private OutpatientVisitHistoryBusinessDelegate delegate;

    @InjectMocks
    private ReceptionServiceImpl service;

    private void enableDelegate() {
        ReflectionTestUtils.setField(service, "outpatientVisitHistoryBusinessDelegate", Optional.of(delegate));
    }

    @Test
    void 외래_진료_기록이_없으면_초진() {
        enableDelegate();
        when(delegate.getVisitHistory("p1")).thenReturn(new OutpatientVisitHistoryResponsedto(false, null));

        VisitTypeResponsedto result = service.getVisitType("p1");

        assertThat(result.isDetermined()).isTrue();
        assertThat(result.getVisitType()).isEqualTo("INITIAL");
        assertThat(result.getLastVisitDate()).isNull();
    }

    @Test
    void 외래_진료_기록이_있으면_재진() {
        enableDelegate();
        when(delegate.getVisitHistory("p1")).thenReturn(new OutpatientVisitHistoryResponsedto(true, "2026-09-30"));

        VisitTypeResponsedto result = service.getVisitType("p1");

        assertThat(result.isDetermined()).isTrue();
        assertThat(result.getVisitType()).isEqualTo("REVISIT");
        assertThat(result.getLastVisitDate()).isEqualTo("2026-09-30");
    }

    @Test
    void 외래_서비스가_실패하면_판정하지_않고_재진으로_간주하지도_않는다() {
        enableDelegate();
        when(delegate.getVisitHistory("p1")).thenThrow(new ResourceAccessException("timeout"));

        VisitTypeResponsedto result = service.getVisitType("p1");

        assertThat(result.isDetermined()).isFalse();
        assertThat(result.getVisitType()).isNull();
    }

    @Test
    void 응답_본문이_없어도_판정하지_않는다() {
        enableDelegate();
        when(delegate.getVisitHistory("p1")).thenReturn(null);

        assertThat(service.getVisitType("p1").isDetermined()).isFalse();
    }

    @Test
    void 연동이_꺼져_있으면_판정하지_않는다() {
        // enableDelegate() 를 부르지 않으면 Optional 필드는 주입되지 않는다 — 빈이 없는 상태와 같다.
        ReflectionTestUtils.setField(service, "outpatientVisitHistoryBusinessDelegate", Optional.empty());

        VisitTypeResponsedto result = service.getVisitType("p1");

        assertThat(result.isDetermined()).isFalse();
        assertThat(result.getVisitType()).isNull();
    }
}

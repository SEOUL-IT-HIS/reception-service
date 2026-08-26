package kr.co.seoulit.his.receptionservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.co.seoulit.his.receptionservice.dto.response.PatientSummaryResponsedto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * TEMP: 접수등록 시 CB2(환자서비스) 환자 목록 조회 REST 호출 확인용.
 * Spring 컨텍스트 없이 순수 HttpClient로 CB2를 직접 호출해 응답 구조를 확인한다.
 * 확인 후 삭제 예정.
 *
 * PatientDelegate(RestTemplate)가 기대하는 반환 타입(List&lt;PatientSummaryResponsedto&gt;)과
 * CB2의 실제 응답 구조가 일치하는지 여기서 먼저 검증한다.
 */
public class PatientListApiManualTest {

    private static final String CB2_URL = "http://192.168.1.149:8080/api/patient/list";

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CB2_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("=== HTTP Status ===");
        System.out.println(response.statusCode());

        System.out.println();
        System.out.println("=== Raw Response Body ===");
        System.out.println(response.body());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        JsonNode root = mapper.readTree(response.body());

        System.out.println();
        System.out.println("=== 응답 최상위 구조 ===");
        System.out.println("최상위가 배열인가? " + root.isArray());
        System.out.println("code/message/data 래퍼 형태인가? " + root.has("data"));

        JsonNode dataNode = root.has("data") ? root.get("data") : root;

        if (dataNode.isArray() && dataNode.size() > 0) {
            System.out.println();
            System.out.println("=== data[0] 실제 필드 목록 ===");
            dataNode.get(0).fieldNames().forEachRemaining(System.out::println);

            System.out.println();
            System.out.println("=== PatientSummaryResponsedto 로 매핑 시도 (Feign이 하는 것과 동일) ===");
            List<PatientSummaryResponsedto> patients = mapper.convertValue(
                    dataNode,
                    mapper.getTypeFactory().constructCollectionType(List.class, PatientSummaryResponsedto.class));

            for (PatientSummaryResponsedto p : patients) {
                System.out.printf(
                        "patientId=%s, patientName=%s, residentRegNo=%s, birthDate=%s, statusCd=%s%n",
                        p.getPatientId(), p.getPatientName(), p.getResidentRegNo(), p.getBirthDate(), p.getStatusCd());
            }
        }
    }
}

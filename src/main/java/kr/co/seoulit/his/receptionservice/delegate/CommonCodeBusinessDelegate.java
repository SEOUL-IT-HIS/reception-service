package kr.co.seoulit.his.receptionservice.delegate;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.dto.response.CommonCodeGroupResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.CommonCodeItemResponsedto;
import lombok.RequiredArgsConstructor;

/**
 * admin-service(공통코드 관리)와 통신하는 RestTemplate 클라이언트
 * - 진료과(DEPT_CD) 등 공통코드 그룹/항목은 여기서만 조회한다.
 */
@Component
@RequiredArgsConstructor
public class CommonCodeBusinessDelegate {

    private final RestTemplate restTemplate;

    @Value("${admin.url}")
    private String adminUrl;

    /** 공통코드 그룹 전체 목록 조회 */
    public ApiResponse<List<CommonCodeGroupResponsedto>> getGroups() {
        ParameterizedTypeReference<ApiResponse<List<CommonCodeGroupResponsedto>>> responseType =
                new ParameterizedTypeReference<>() {
                };
        return restTemplate.exchange(
                adminUrl + "/api/commonCodeGroup/list",
                HttpMethod.GET,
                null,
                responseType).getBody();
    }

    /** 그룹ID 기준 공통코드 항목 목록 조회 */
    public ApiResponse<List<CommonCodeItemResponsedto>> getItems(String groupId) {
        ParameterizedTypeReference<ApiResponse<List<CommonCodeItemResponsedto>>> responseType =
                new ParameterizedTypeReference<>() {
                };
        String url = UriComponentsBuilder.fromUriString(adminUrl + "/api/commonCodeItem/list")
                .queryParam("groupId", groupId)
                .toUriString();
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                responseType).getBody();
    }

}

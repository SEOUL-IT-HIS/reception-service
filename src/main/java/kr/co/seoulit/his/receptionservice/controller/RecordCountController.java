package kr.co.seoulit.his.receptionservice.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.service.RecordCountService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reception")
@RequiredArgsConstructor
public class RecordCountController {

    private final RecordCountService recordCountService;

    @GetMapping("/record-count/{receptionId}")
    public ApiResponse<Map<String, Object>> countReceptions(@PathVariable String receptionId) {
        Map<String, Object> result = recordCountService.countByReceptionId(receptionId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("receptionId", receptionId);
        response.put("count", result.get("recordCount"));

        return ApiResponse.success(HttpStatus.OK.value(), response);
    }
}

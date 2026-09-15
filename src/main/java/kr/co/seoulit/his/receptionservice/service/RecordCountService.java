package kr.co.seoulit.his.receptionservice.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import kr.co.seoulit.his.receptionservice.common.mybatis.ProcMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordCountService {

    private final ProcMapper procMapper;

    public Map<String, Object> countByReceptionId(String receptionId) {
        Map<String, Object> param = new HashMap<>();

        param.put("receptionId", receptionId);

        log.info("프로시저 실행 전");
        log.info("접수ID: {}", param.get("receptionId"));

        procMapper.countByReception(param);

        log.info("프로시저 실행 후");
        log.info("접수 건수: {}", param.get("recordCount"));

        return param;
    }
}

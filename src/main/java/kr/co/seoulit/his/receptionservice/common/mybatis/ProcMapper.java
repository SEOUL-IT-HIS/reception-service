package kr.co.seoulit.his.receptionservice.common.mybatis;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * Oracle 저장 프로시저 호출 전용 Mapper.
 * CALLABLE 문은 OUT 파라미터를 param Map 에 되돌려 담는다.
 */
@Mapper
public interface ProcMapper {

    /**
     * PRC_COUNT_RECEPTION 프로시저 호출.
     * param: { receptionId(IN) } → 실행 후 { recordCount(OUT) } 이 채워진다.
     */
    void countByReception(Map<String, Object> param);
}

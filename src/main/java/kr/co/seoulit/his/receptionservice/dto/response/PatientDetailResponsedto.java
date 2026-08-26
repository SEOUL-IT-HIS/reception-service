package kr.co.seoulit.his.receptionservice.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDetailResponsedto {

    /** 환자 ID (CB2 UUID) */
    private String patientId;

    /** 환자명 */
    private String patientName;

    /** 생년월일 */
    private LocalDate birthDate;

    /** 성별 */
    private String gender;

    /** 연락처 */
    private String phoneNumber;

    /** 주소 */
    private String address;
}
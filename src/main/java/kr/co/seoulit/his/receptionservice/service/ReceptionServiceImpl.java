package kr.co.seoulit.his.receptionservice.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.seoulit.his.receptionservice.cache.CommonCodeCache;
import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.delegate.DoctorBusinessDelegate;
import kr.co.seoulit.his.receptionservice.dto.request.EmergencyReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionCancelRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionStatusChangeRequestdto;
import kr.co.seoulit.his.receptionservice.dto.response.CommonCodeItemResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.DepartmentResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.DoctorResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.EmergencyReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReceptionDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.entity.EmergencyInfoEntity;
import kr.co.seoulit.his.receptionservice.entity.ReceptionCancelEntity;
import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;
import kr.co.seoulit.his.receptionservice.entity.ReceptionStatusHistoryEntity;
import kr.co.seoulit.his.receptionservice.exception.EmergencyInfoRequiredException;
import kr.co.seoulit.his.receptionservice.exception.ReceptionAlreadyCancelledException;
import kr.co.seoulit.his.receptionservice.exception.ReceptionNotFoundException;
import kr.co.seoulit.his.receptionservice.exception.ReceptionStatusUnchangedException;
import kr.co.seoulit.his.receptionservice.repository.EmergencyInfoRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionCancelRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionStatusHistoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceptionServiceImpl implements ReceptionService {

    private static final Logger log = LoggerFactory.getLogger(ReceptionServiceImpl.class);

    private static final String RECEPTION_TYPE_EMERGENCY = "EMERGENCY";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String DEPT_CD_GROUP = "DEPT_CD";
    private static final DateTimeFormatter RECEPTION_NO_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ReceptionRepository receptionRepository;
    private final EmergencyInfoRepository emergencyInfoRepository;
    private final ReceptionCancelRepository receptionCancelRepository;
    private final ReceptionStatusHistoryRepository receptionStatusHistoryRepository;
    private final CommonCodeCache commonCodeCache;
    private final DoctorBusinessDelegate doctorBusinessDelegate;

    /**
     * 접수 등록
     */
    @Override
    @Transactional
    public void registerReception(ReceptionRequestdto request) {

        LocalDateTime now = LocalDateTime.now();

        ReceptionEntity reception = createReceptionEntity(
                request.getPatientId(),
                request.getDeptId(),
                request.getDoctorId(),
                request.getReceptionType(),
                request.getMemo(),
                now);

        receptionRepository.save(reception);
    }

    /**
     * 응급접수 등록
     */
    @Override
    @Transactional
    public EmergencyReceptionResponsedto registerEmergencyReception(EmergencyReceptionRequestdto request) {

        if (request.getKtasLevel() == null || request.getChiefComplaint() == null) {
            throw new EmergencyInfoRequiredException();
        }

        LocalDateTime now = LocalDateTime.now();

        ReceptionEntity reception = createReceptionEntity(
                request.getPatientId(),
                request.getDeptId(),
                request.getDoctorId(),
                RECEPTION_TYPE_EMERGENCY,
                request.getMemo(),
                now);

        receptionRepository.save(reception);

        EmergencyInfoEntity emergencyInfo = EmergencyInfoEntity.builder()
                .receptionId(reception.getReceptionId())
                .ktasLevel(request.getKtasLevel())
                .visitMethod(request.getVisitMethod())
                .chiefComplaint(request.getChiefComplaint())
                .consciousness(request.getConsciousness())
                .triageDateTime(now)
                .build();

        emergencyInfoRepository.save(emergencyInfo);

        return toEmergencyResponseDto(reception, emergencyInfo);
    }

    /**
     * 접수(RECEPTION) Entity 생성
     */
    private ReceptionEntity createReceptionEntity(
            String patientId, Long deptId, String doctorId, String receptionType, String memo, LocalDateTime now) {

        return ReceptionEntity.builder()
                .patientId(patientId)
                .deptId(deptId)
                .doctorId(doctorId)
                .receptionNo(generateReceptionNo(now))
                .receptionType(receptionType)
                .status("RECEPTION") // 최초 상태
                .memo(memo)
                .receptionDate(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /** 접수번호 생성 — RECEPTION_NO 는 DB에서 NOT NULL 이라 저장 전에 채워야 한다 */
    private String generateReceptionNo(LocalDateTime now) {
        return "R" + now.format(RECEPTION_NO_FORMAT);
    }

    /**
     * 접수 목록 조회
     * - 진료과명(공통코드 캐시)/의사명(admin-service)을 조회해 함께 채운다.
     * - 환자명은 여기서 채우지 않는다 (프론트가 patientId 로 CB2 batch 조회해서 직접 조합).
     */
    @Override
    public List<ReceptionResponsedto> getReceptionList() {
        List<ReceptionEntity> receptions = receptionRepository.findAllByOrderByReceptionDateDesc();

        Map<Long, String> deptNames = getDeptNames();
        Map<String, String> doctorNames = getDoctorNames(receptions);

        return receptions.stream()
                .map(reception -> toResponseDto(reception, deptNames, doctorNames))
                .toList();
    }

    /**
     * 진료과ID → 진료과명 (공통코드 로컬캐시(DEPT_CD)에서 바로 읽음 — admin-service 호출 없음)
     */
    private Map<Long, String> getDeptNames() {
        Map<Long, String> deptNames = new HashMap<>();
        for (CommonCodeItemResponsedto item : commonCodeCache.getItems(DEPT_CD_GROUP)) {
            Long deptId = parseDeptId(item.getCodeValue());
            if (deptId == null) {
                continue;
            }
            deptNames.put(deptId, item.getCodeName());
        }
        return deptNames;
    }

    /** DEPT_CD 공통코드의 codeValue → RECEPTION.DEPT_ID(NUMBER) 로 변환 */
    private Long parseDeptId(String codeValue) {
        try {
            return Long.parseLong(codeValue);
        } catch (NumberFormatException e) {
            log.warn("DEPT_CD codeValue를 숫자로 변환할 수 없음 (건너뜀): {}", codeValue);
            return null;
        }
    }

    /**
     * 의사ID → 의사명 (목록에 등장하는 진료과별로 1회씩만 조회해 맵 구성)
     * admin-service가 응답하지 않아도 접수 목록 자체는 볼 수 있어야 하므로, 실패한 진료과는 건너뛴다.
     */
    private Map<String, String> getDoctorNames(List<ReceptionEntity> receptions) {
        Set<Long> deptIds = receptions.stream()
                .map(ReceptionEntity::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> doctorNames = new HashMap<>();
        for (Long deptId : deptIds) {
            try {
                for (DoctorResponsedto doctor : doctorBusinessDelegate.getDoctorsByDepartment(deptId).data()) {
                    doctorNames.put(doctor.getDoctorId(), doctor.getDoctorName());
                }
            } catch (Exception e) {
                log.warn("의사명 조회 실패 - deptId={} admin-service 응답 없음, 해당 진료과는 건너뛴다", deptId, e);
            }
        }
        return doctorNames;
    }

    /**
     * 접수 상세 조회
     * - 진료과명(공통코드 캐시)만 함께 채운다. 환자명은 프론트가 CB2 batch 조회로 조합한다.
     */
    @Override
    public ReceptionDetailResponsedto getReception(Long receptionId) {

        ReceptionEntity reception = receptionRepository.findById(receptionId)
                .orElseThrow(() -> new ReceptionNotFoundException(receptionId));

        String deptName = getDeptNames().get(reception.getDeptId());

        return toDetailResponseDto(reception, deptName);
    }

    /**
     * 접수 상태 변경
     */
    @Override
    @Transactional
    public void changeReceptionStatus(Long receptionId, ReceptionStatusChangeRequestdto request) {

        ReceptionEntity reception = receptionRepository.findById(receptionId)
                .orElseThrow(() -> new ReceptionNotFoundException(receptionId));

        String prevStatus = reception.getStatus();

        if (STATUS_CANCELLED.equals(prevStatus)) {
            throw new ReceptionAlreadyCancelledException(receptionId);
        }

        if (prevStatus.equals(request.getNewStatus())) {
            throw new ReceptionStatusUnchangedException(receptionId, prevStatus);
        }

        LocalDateTime now = LocalDateTime.now();

        reception.changeStatus(request.getNewStatus(), now);

        ReceptionStatusHistoryEntity history = ReceptionStatusHistoryEntity.builder()
                .receptionId(receptionId)
                .prevStatus(prevStatus)
                .newStatus(request.getNewStatus())
                .changedBy(request.getChangedBy())
                .changedAt(now)
                .reason(request.getReason())
                .build();

        receptionStatusHistoryRepository.save(history);
    }

    /**
     * 접수 취소
     */
    @Override
    @Transactional
    public void cancelReception(Long receptionId, ReceptionCancelRequestdto request) {

        ReceptionEntity reception = receptionRepository.findById(receptionId)
                .orElseThrow(() -> new ReceptionNotFoundException(receptionId));

        String prevStatus = reception.getStatus();

        if (STATUS_CANCELLED.equals(prevStatus)) {
            throw new ReceptionAlreadyCancelledException(receptionId);
        }

        LocalDateTime now = LocalDateTime.now();

        reception.changeStatus(STATUS_CANCELLED, now);

        ReceptionCancelEntity cancel = ReceptionCancelEntity.builder()
                .receptionId(receptionId)
                .cancelReasonCode(request.getCancelReasonCode())
                .cancelReasonDetail(request.getCancelReasonDetail())
                .cancelledBy(request.getCancelledBy())
                .cancelledAt(now)
                .build();

        receptionCancelRepository.save(cancel);

        ReceptionStatusHistoryEntity history = ReceptionStatusHistoryEntity.builder()
                .receptionId(receptionId)
                .prevStatus(prevStatus)
                .newStatus(STATUS_CANCELLED)
                .changedBy(request.getCancelledBy())
                .changedAt(now)
                .reason(request.getCancelReasonDetail())
                .build();

        receptionStatusHistoryRepository.save(history);
    }

    private ReceptionResponsedto toResponseDto(
            ReceptionEntity reception,
            Map<Long, String> deptNames,
            Map<String, String> doctorNames) {
        return ReceptionResponsedto.builder()
                .receptionId(reception.getReceptionId())
                .receptionNo(reception.getReceptionNo())
                .patientId(reception.getPatientId())
                .deptId(reception.getDeptId())
                .deptName(deptNames.get(reception.getDeptId()))
                .doctorId(reception.getDoctorId())
                .doctorName(doctorNames.get(reception.getDoctorId()))
                .receptionType(reception.getReceptionType())
                .status(reception.getStatus())
                .receptionDate(reception.getReceptionDate())
                .build();
    }

    private ReceptionDetailResponsedto toDetailResponseDto(
            ReceptionEntity reception, String deptName) {
        return ReceptionDetailResponsedto.builder()
                .receptionId(reception.getReceptionId())
                .patientId(reception.getPatientId())
                .deptId(reception.getDeptId())
                .deptName(deptName)
                .doctorId(reception.getDoctorId())
                .receptionNo(reception.getReceptionNo())
                .receptionType(reception.getReceptionType())
                .status(reception.getStatus())
                .memo(reception.getMemo())
                .receptionDate(reception.getReceptionDate())
                .createdAt(reception.getCreatedAt())
                .updatedAt(reception.getUpdatedAt())
                .build();
    }

    private EmergencyReceptionResponsedto toEmergencyResponseDto(
            ReceptionEntity reception, EmergencyInfoEntity emergencyInfo) {

        return EmergencyReceptionResponsedto.builder()
                .receptionId(reception.getReceptionId())
                .patientId(reception.getPatientId())
                .deptId(reception.getDeptId())
                .doctorId(reception.getDoctorId())
                .receptionNo(reception.getReceptionNo())
                .receptionType(reception.getReceptionType())
                .status(reception.getStatus())
                .memo(reception.getMemo())
                .receptionDate(reception.getReceptionDate())
                .ktasLevel(emergencyInfo.getKtasLevel())
                .visitMethod(emergencyInfo.getVisitMethod())
                .chiefComplaint(emergencyInfo.getChiefComplaint())
                .consciousness(emergencyInfo.getConsciousness())
                .triageDateTime(emergencyInfo.getTriageDateTime())
                .build();
    }

    /**
     * 진료과 목록 조회 (공통코드 로컬캐시 DEPT_CD 그룹)
     */
    @Override
    public ApiResponse<List<DepartmentResponsedto>> getDepartments() {
        List<DepartmentResponsedto> departments = commonCodeCache.getItems(DEPT_CD_GROUP).stream()
                .map(item -> DepartmentResponsedto.builder()
                        .deptId(parseDeptId(item.getCodeValue()))
                        .deptName(item.getCodeName())
                        .build())
                .filter(dept -> dept.getDeptId() != null)
                .toList();

        return ApiResponse.success(200, departments);
    }

    /**
     * 진료과별 의사 목록 조회
     */
    @Override
    public ApiResponse<List<DoctorResponsedto>> getDoctorsByDepartment(Long deptId) {
        return doctorBusinessDelegate.getDoctorsByDepartment(deptId);
    }

}

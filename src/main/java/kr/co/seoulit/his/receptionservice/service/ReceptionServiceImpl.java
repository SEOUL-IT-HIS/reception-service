package kr.co.seoulit.his.receptionservice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import kr.co.seoulit.his.receptionservice.cache.CommonCodeCache;
import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.delegate.DoctorBusinessDelegate;
import kr.co.seoulit.his.receptionservice.delegate.PatientBusinessDelegate;
import kr.co.seoulit.his.receptionservice.dto.request.EmergencyReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionCancelRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionStatusChangeRequestdto;
import kr.co.seoulit.his.receptionservice.dto.response.CommonCodeItemResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.DepartmentResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.DoctorResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.EmergencyReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.PatientDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.PatientSummaryResponsedto;
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
import kr.co.seoulit.his.receptionservice.messaging.event.EmergencyReceptionRegisteredInternalEvent;
import kr.co.seoulit.his.receptionservice.messaging.event.ReceptionRegisteredInternalEvent;
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
    /** 의사명 병렬 조회 시 진료과 1건당 최대 대기시간. admin-service가 느리거나 죽어있어도 이 이상은 안 기다린다. */
    private static final long ADMIN_LOOKUP_TIMEOUT_SECONDS = 5;

    private final ReceptionRepository receptionRepository;
    private final EmergencyInfoRepository emergencyInfoRepository;
    private final ReceptionCancelRepository receptionCancelRepository;
    private final ReceptionStatusHistoryRepository receptionStatusHistoryRepository;
    private final CommonCodeCache commonCodeCache;
    private final DoctorBusinessDelegate doctorBusinessDelegate;
    private final PatientBusinessDelegate patientBusinessDelegate;
    private final ApplicationEventPublisher eventPublisher;
    private final ExecutorService adminLookupExecutor;

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

        publishOutpatientReceptionRegistered(reception);
    }

    /**
     * 외래 접수(응급이 아닌 모든 접수)인 경우, 트랜잭션 커밋 후 외래 서비스로 접수 등록 이벤트를
     * 발행하도록 예약한다. (실제 Kafka 전송은 {@code @TransactionalEventListener(AFTER_COMMIT)} 에서 수행)
     *
     * <p>이 메서드가 호출되는 {@link #registerReception}은 외래 접수 전용 경로다
     * (응급 접수는 {@link #registerEmergencyReception} 를 통하며 이 이벤트를 발행하지 않는다).
     * 접수유형은 초진/재진(INITIAL/REVISIT) 등으로 들어오므로, 응급만 방어적으로 제외한다.
     */
    private void publishOutpatientReceptionRegistered(ReceptionEntity reception) {
        if (RECEPTION_TYPE_EMERGENCY.equals(reception.getReceptionType())) {
            return;
        }
        eventPublisher.publishEvent(ReceptionRegisteredInternalEvent.from(reception));
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

        // 응급 서비스로 넘기는 목록에 환자명을 함께 담는다.
        // CB2 장애로 응급접수 자체가 막히면 안 되므로, 조회 실패 시 환자명 없이 진행한다.
        String patientName = resolvePatientName(request.getPatientId());

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
                .emergencyInfoId(UUID.randomUUID().toString())
                .receptionId(reception.getReceptionId())
                .ktasLevel(request.getKtasLevel())
                .visitMethod(request.getVisitMethod())
                .chiefComplaint(request.getChiefComplaint())
                .consciousness(request.getConsciousness())
                .triageDateTime(now)
                .build();

        emergencyInfoRepository.save(emergencyInfo);

        // 트랜잭션 커밋 후 응급 서비스로 접수내역을 REST 전송하도록 예약한다.
        // (실제 전송은 EmergencyReceptionIntakeListener 의 @TransactionalEventListener(AFTER_COMMIT) 에서 수행)
        eventPublisher.publishEvent(new EmergencyReceptionRegisteredInternalEvent(
                reception.getReceptionId(),
                reception.getPatientId(),
                request.getVisitMethod(),
                reception.getReceptionDate(),
                reception.getMemo(),
                request.getChiefComplaint()));

        return toEmergencyResponseDto(reception, emergencyInfo, patientName,
                getDeptNames().get(reception.getDeptId()), null);
    }

    /**
     * 응급접수 목록 조회 (응급접수홈 전용)
     * - 당일(00:00~익일 00:00) 접수된 응급 건 중 취소 제외, 접수일시 최신순.
     * - EMERGENCY_INFO(KTAS·내원경로·주호소 등)를 조인하고, 진료과명/의사명/환자명을 서버에서 채운다.
     */
    @Override
    public List<EmergencyReceptionResponsedto> getEmergencyReceptionList() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        List<ReceptionEntity> receptions = receptionRepository
                .findByReceptionTypeAndReceptionDateBetween(RECEPTION_TYPE_EMERGENCY, startOfDay, startOfDay.plusDays(1))
                .stream()
                .filter(reception -> !STATUS_CANCELLED.equals(reception.getStatus()))
                .sorted(Comparator.comparing(ReceptionEntity::getReceptionDate).reversed())
                .toList();

        if (receptions.isEmpty()) {
            return List.of();
        }

        List<String> receptionIds = receptions.stream().map(ReceptionEntity::getReceptionId).toList();
        Map<String, EmergencyInfoEntity> emergencyInfos = emergencyInfoRepository.findByReceptionIdIn(receptionIds).stream()
                .collect(Collectors.toMap(EmergencyInfoEntity::getReceptionId, info -> info, (a, b) -> a));

        Map<String, String> deptNames = getDeptNames();
        Map<String, String> doctorNames = getDoctorNames(receptions);
        Map<String, String> patientNames = getPatientNames();

        return receptions.stream()
                .map(reception -> toEmergencyResponseDto(
                        reception,
                        emergencyInfos.get(reception.getReceptionId()),
                        patientNames.get(reception.getPatientId()),
                        deptNames.get(reception.getDeptId()),
                        doctorNames.get(reception.getDoctorId())))
                .toList();
    }

    /**
     * CB2 환자 목록을 1회 조회해 patientId → 환자명 맵을 만든다.
     * CB2 장애 시 빈 맵을 반환한다 (목록은 환자명 없이 정상 응답).
     */
    private Map<String, String> getPatientNames() {
        try {
            ApiResponse<List<PatientSummaryResponsedto>> response = patientBusinessDelegate.getPatients();
            if (response == null || response.data() == null) {
                return Map.of();
            }
            Map<String, String> patientNames = new HashMap<>();
            for (PatientSummaryResponsedto patient : response.data()) {
                if (patient.getPatientId() != null) {
                    patientNames.put(patient.getPatientId(), patient.getPatientName());
                }
            }
            return patientNames;
        } catch (RestClientException e) {
            log.warn("CB2 환자 목록 조회 실패 - 환자명 없이 응급접수 목록 반환", e);
            return Map.of();
        }
    }

    /**
     * 접수(RECEPTION) Entity 생성
     */
    private ReceptionEntity createReceptionEntity(
            String patientId, String deptId, String doctorId, String receptionType, String memo, LocalDateTime now) {

        return ReceptionEntity.builder()
                .receptionId(UUID.randomUUID().toString())
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
     * 접수 목록 조회 (접수홈용 — 당일 등록 건만, 응급접수 제외)
     * - 진료과명(공통코드 캐시)/의사명(admin-service)을 조회해 함께 채운다.
     * - 환자명은 여기서 채우지 않는다 (프론트가 patientId 로 CB2 batch 조회해서 직접 조합).
     * - 응급 건은 응급접수홈 전용 목록({@link #getEmergencyReceptionList()})에서만 보여준다.
     */
    @Override
    public List<ReceptionResponsedto> getReceptionList() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        List<ReceptionEntity> receptions = receptionRepository
                .findByReceptionTypeNotAndReceptionDateBetweenOrderByReceptionDateDesc(
                        RECEPTION_TYPE_EMERGENCY, startOfDay, startOfDay.plusDays(1));

        Map<String, String> deptNames = getDeptNames();
        Map<String, String> doctorNames = getDoctorNames(receptions);

        return receptions.stream()
                .map(reception -> toResponseDto(reception, deptNames, doctorNames))
                .toList();
    }

    /**
     * 진료과ID → 진료과명 (공통코드 로컬캐시(DEPT_CD)에서 바로 읽음 — admin-service 호출 없음)
     * DEPT_ID 는 DEPT_CD 공통코드의 codeValue 를 그대로 저장한다 (문자열).
     */
    private Map<String, String> getDeptNames() {
        Map<String, String> deptNames = new HashMap<>();
        for (CommonCodeItemResponsedto item : commonCodeCache.getItems(DEPT_CD_GROUP)) {
            deptNames.put(item.getCodeValue(), item.getCodeName());
        }
        return deptNames;
    }

    /**
     * 의사ID → 의사명 (목록에 등장하는 진료과별로 1회씩만 조회해 맵 구성)
     * admin-service가 응답하지 않아도 접수 목록 자체는 볼 수 있어야 하므로, 실패한 진료과는 건너뛴다.
     *
     * <p>진료과 수만큼 admin-service를 순차 호출하면 "진료과 수 × 커넥트타임아웃"만큼 걸려
     * 프론트 타임아웃을 넘기기 쉽다(예: 6개 진료과 × 3초 ≈ 18초). 병렬로 물어봐서 전체 대기시간을
     * 타임아웃 1회 수준으로 줄인다. admin-service 세션 쿠키는 요청 스레드에만 있으므로, 병렬로
     * 실행할 워커 스레드에 명시적으로 복사해 넘긴다({@link OutboundSessionForwardingInterceptor}
     * 가 그 쿠키를 읽어 admin-service 호출에 실어 보낸다).
     */
    private Map<String, String> getDoctorNames(List<ReceptionEntity> receptions) {
        Set<String> deptIds = receptions.stream()
                .map(ReceptionEntity::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        RequestAttributes callerRequest = RequestContextHolder.getRequestAttributes();

        List<CompletableFuture<List<DoctorResponsedto>>> lookups = deptIds.stream()
                .map(deptId -> CompletableFuture.supplyAsync(
                        () -> fetchDoctorsForDept(deptId, callerRequest), adminLookupExecutor))
                .toList();

        Map<String, String> doctorNames = new HashMap<>();
        for (CompletableFuture<List<DoctorResponsedto>> lookup : lookups) {
            try {
                for (DoctorResponsedto doctor : lookup.get(ADMIN_LOOKUP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    doctorNames.put(doctor.getDoctorId(), doctor.getDoctorName());
                }
            } catch (Exception e) {
                log.warn("의사명 조회 실패, 해당 진료과는 건너뛴다 (admin-service: {})", e.getMessage());
            }
        }
        return doctorNames;
    }

    private List<DoctorResponsedto> fetchDoctorsForDept(String deptId, RequestAttributes callerRequest) {
        if (callerRequest != null) {
            RequestContextHolder.setRequestAttributes(callerRequest);
        }
        try {
            return doctorBusinessDelegate.getDoctorsByDepartment(deptId).data();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * 접수 상세 조회
     * - 진료과명(공통코드 캐시)만 함께 채운다. 환자명은 프론트가 CB2 batch 조회로 조합한다.
     */
    @Override
    public ReceptionDetailResponsedto getReception(String receptionId) {

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
    public void changeReceptionStatus(String receptionId, ReceptionStatusChangeRequestdto request) {

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
                .historyId(UUID.randomUUID().toString())
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
    public void cancelReception(String receptionId, ReceptionCancelRequestdto request) {

        ReceptionEntity reception = receptionRepository.findById(receptionId)
                .orElseThrow(() -> new ReceptionNotFoundException(receptionId));

        String prevStatus = reception.getStatus();

        if (STATUS_CANCELLED.equals(prevStatus)) {
            throw new ReceptionAlreadyCancelledException(receptionId);
        }

        LocalDateTime now = LocalDateTime.now();

        reception.changeStatus(STATUS_CANCELLED, now);

        ReceptionCancelEntity cancel = ReceptionCancelEntity.builder()
                .cancelId(UUID.randomUUID().toString())
                .receptionId(receptionId)
                .cancelReasonCode(request.getCancelReasonCode())
                .cancelReasonDetail(request.getCancelReasonDetail())
                .cancelledBy(request.getCancelledBy())
                .cancelledAt(now)
                .build();

        receptionCancelRepository.save(cancel);

        ReceptionStatusHistoryEntity history = ReceptionStatusHistoryEntity.builder()
                .historyId(UUID.randomUUID().toString())
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
            Map<String, String> deptNames,
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

    /**
     * CB2 에서 환자명을 조회한다. 조회 실패(장애/미존재)는 응급접수를 막지 않고 null 로 넘어간다.
     */
    private String resolvePatientName(String patientId) {
        try {
            PatientDetailResponsedto patient = patientBusinessDelegate.getPatientById(patientId);
            return patient != null ? patient.getPatientName() : null;
        } catch (RestClientException e) {
            log.warn("CB2 환자명 조회 실패 - 환자명 없이 응급접수 진행. patientId={}", patientId, e);
            return null;
        }
    }

    private EmergencyReceptionResponsedto toEmergencyResponseDto(
            ReceptionEntity reception, EmergencyInfoEntity emergencyInfo, String patientName,
            String deptName, String doctorName) {

        EmergencyReceptionResponsedto.EmergencyReceptionResponsedtoBuilder builder = EmergencyReceptionResponsedto.builder()
                .receptionId(reception.getReceptionId())
                .patientId(reception.getPatientId())
                .patientName(patientName)
                .deptId(reception.getDeptId())
                .deptName(deptName)
                .doctorId(reception.getDoctorId())
                .doctorName(doctorName)
                .receptionNo(reception.getReceptionNo())
                .receptionType(reception.getReceptionType())
                .status(reception.getStatus())
                .memo(reception.getMemo())
                .receivedAt(reception.getReceptionDate());

        if (emergencyInfo != null) {
            builder.ktasLevel(emergencyInfo.getKtasLevel())
                    .arrivalPath(emergencyInfo.getVisitMethod())
                    .chiefComplaintRaw(emergencyInfo.getChiefComplaint())
                    .consciousness(emergencyInfo.getConsciousness())
                    .triageDateTime(emergencyInfo.getTriageDateTime());
        }

        return builder.build();
    }

    /**
     * 진료과 목록 조회 (공통코드 로컬캐시 DEPT_CD 그룹)
     */
    @Override
    public ApiResponse<List<DepartmentResponsedto>> getDepartments() {
        List<DepartmentResponsedto> departments = commonCodeCache.getItems(DEPT_CD_GROUP).stream()
                .map(item -> DepartmentResponsedto.builder()
                        .deptId(item.getCodeValue())
                        .deptName(item.getCodeName())
                        .build())
                .toList();

        return ApiResponse.success(200, departments);
    }

    /**
     * 진료과별 의사 목록 조회
     */
    @Override
    public ApiResponse<List<DoctorResponsedto>> getDoctorsByDepartment(String deptId) {
        return doctorBusinessDelegate.getDoctorsByDepartment(deptId);
    }

}

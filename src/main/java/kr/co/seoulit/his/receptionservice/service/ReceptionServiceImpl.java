package kr.co.seoulit.his.receptionservice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import kr.co.seoulit.his.receptionservice.cache.CommonCodeCache;
import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.delegate.DoctorBusinessDelegate;
import kr.co.seoulit.his.receptionservice.delegate.EmergencyActiveCheckBusinessDelegate;
import kr.co.seoulit.his.receptionservice.delegate.EmergencyCancelCheckBusinessDelegate;
import kr.co.seoulit.his.receptionservice.delegate.EmpBusinessDelegate;
import kr.co.seoulit.his.receptionservice.delegate.OutpatientVisitHistoryBusinessDelegate;
import kr.co.seoulit.his.receptionservice.delegate.PatientBusinessDelegate;
import kr.co.seoulit.his.receptionservice.dto.request.EmergencyReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionCancelRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionStatusChangeRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReservationRequestdto;
import kr.co.seoulit.his.receptionservice.dto.response.ActiveEmergencyReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.CommonCodeItemResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.DepartmentResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.DoctorResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.EmergencyActiveCheckResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.EmergencyCancellableResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.EmergencyReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.EmpResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.OutpatientVisitHistoryResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.PatientDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.PatientSummaryResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReceptionDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReservationResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.VisitTypeResponsedto;
import kr.co.seoulit.his.receptionservice.entity.EmergencyInfoEntity;
import kr.co.seoulit.his.receptionservice.entity.ReceptionCancelEntity;
import kr.co.seoulit.his.receptionservice.entity.ReceptionEntity;
import kr.co.seoulit.his.receptionservice.entity.ReceptionStatusHistoryEntity;
import kr.co.seoulit.his.receptionservice.entity.ReservationEntity;
import kr.co.seoulit.his.receptionservice.exception.CancelViaStatusChangeNotAllowedException;
import kr.co.seoulit.his.receptionservice.exception.DoctorRequiredException;
import kr.co.seoulit.his.receptionservice.exception.EmergencyCancelCheckFailedException;
import kr.co.seoulit.his.receptionservice.exception.EmergencyCancelNotAllowedException;
import kr.co.seoulit.his.receptionservice.exception.EmergencyInfoRequiredException;
import kr.co.seoulit.his.receptionservice.exception.InvalidReceptionTypeException;
import kr.co.seoulit.his.receptionservice.exception.InvalidReservationException;
import kr.co.seoulit.his.receptionservice.exception.ReceptionAlreadyCancelledException;
import kr.co.seoulit.his.receptionservice.exception.ReceptionNotFoundException;
import kr.co.seoulit.his.receptionservice.exception.ReceptionStatusUnchangedException;
import kr.co.seoulit.his.receptionservice.exception.ReservationNotFoundException;
import kr.co.seoulit.his.receptionservice.exception.ReservationNotReceivableException;
import kr.co.seoulit.his.receptionservice.messaging.event.EmergencyReceptionCancelledInternalEvent;
import kr.co.seoulit.his.receptionservice.messaging.event.EmergencyReceptionRegisteredInternalEvent;
import kr.co.seoulit.his.receptionservice.messaging.event.ReceptionCancelledInternalEvent;
import kr.co.seoulit.his.receptionservice.messaging.event.ReceptionRegisteredInternalEvent;
import kr.co.seoulit.his.receptionservice.repository.EmergencyInfoRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionCancelRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionRepository;
import kr.co.seoulit.his.receptionservice.repository.ReceptionStatusHistoryRepository;
import kr.co.seoulit.his.receptionservice.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceptionServiceImpl implements ReceptionService {

    private static final Logger log = LoggerFactory.getLogger(ReceptionServiceImpl.class);

    private static final String RECEPTION_TYPE_EMERGENCY = "EMERGENCY";
    /** 외래 접수유형 — 예약 목록의 "접수하기"로 만든 접수는 RESERVATION(예약), 직접 입력한 접수는 WALK_IN(당일) */
    private static final String RECEPTION_TYPE_RESERVATION = "RESERVATION";
    private static final String RECEPTION_TYPE_WALK_IN = "WALK_IN";
    private static final Pattern RESERVATION_TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    /** 초진/재진 — INITIAL(초진), REVISIT(재진) */
    private static final Set<String> VISIT_TYPES = Set.of("INITIAL", "REVISIT");
    /** 응급 취소 가능 여부 조회의 reasonCode — 진료 기록 있음 */
    private static final String EMERGENCY_CANCEL_HAS_RECORDS = "HAS_RECORDS";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String DEPT_CD_GROUP = "DEPT_CD";
    private static final DateTimeFormatter RECEPTION_NO_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    /** 담당의 ID 최대 길이 — RECEPTION.DOCTOR_ID, 외래 DOCTOR_ID/PRESCRIBED_BY 모두 length 36 */
    private static final int DOCTOR_ID_MAX_LENGTH = 36;

    private final ReceptionRepository receptionRepository;
    private final EmergencyInfoRepository emergencyInfoRepository;
    private final ReceptionCancelRepository receptionCancelRepository;
    private final ReceptionStatusHistoryRepository receptionStatusHistoryRepository;
    private final ReservationRepository reservationRepository;
    private final CommonCodeCache commonCodeCache;
    private final DoctorBusinessDelegate doctorBusinessDelegate;
    private final EmpBusinessDelegate empBusinessDelegate;
    private final PatientBusinessDelegate patientBusinessDelegate;
    /** emergency.active-check.enabled=false 면 빈이 없으므로 Optional — 없으면 fail-open으로 건너뛴다. */
    private final Optional<EmergencyActiveCheckBusinessDelegate> emergencyActiveCheckBusinessDelegate;
    /** emergency.cancel-check.enabled=false 면 빈이 없으므로 Optional — 없으면 확인 없이 취소한다(응급 배포 전). */
    private final Optional<EmergencyCancelCheckBusinessDelegate> emergencyCancelCheckBusinessDelegate;
    /** outpatient.visit-history.enabled=false 면 빈이 없으므로 Optional — 없으면 초진/재진을 판정하지 않는다(외래 API 배포 전). */
    private final Optional<OutpatientVisitHistoryBusinessDelegate> outpatientVisitHistoryBusinessDelegate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 접수 등록
     */
    @Override
    @Transactional
    public void registerReception(ReceptionRequestdto request) {

        String doctorId = requireDoctorId(request.getDoctorId());

        if (!VISIT_TYPES.contains(request.getVisitType())) {
            throw new InvalidReceptionTypeException(
                    "초진/재진은 INITIAL/REVISIT만 가능합니다. visitType=" + request.getVisitType());
        }

        // 접수유형은 요청 값이 아니라 경로로 정한다: 예약 목록의 "접수하기"로 들어오면 예약, 직접 입력하면 당일.
        // 같은 예약으로 접수가 두 번 만들어지지 않도록 예약 행을 잠그고 상태를 확인한다.
        ReservationEntity reservation = null;
        String receptionType = RECEPTION_TYPE_WALK_IN;
        if (request.getReservationId() != null && !request.getReservationId().isBlank()) {
            reservation = reservationRepository.findByIdForUpdate(request.getReservationId())
                    .orElseThrow(() -> new ReservationNotFoundException(request.getReservationId()));
            if (!ReservationEntity.STATUS_RESERVED.equals(reservation.getStatus())) {
                throw new ReservationNotReceivableException(reservation.getReservationId(), "예약됨(RESERVED) 상태가 아닙니다. status=" + reservation.getStatus());
            }
            if (!reservation.getPatientId().equals(request.getPatientId())) {
                throw new ReservationNotReceivableException(reservation.getReservationId(), "예약의 환자와 다릅니다.");
            }
            receptionType = RECEPTION_TYPE_RESERVATION;
        }

        LocalDateTime now = LocalDateTime.now();

        ReceptionEntity reception = createReceptionEntity(
                request.getPatientId(),
                request.getDeptId(),
                doctorId,
                receptionType,
                request.getVisitType(),
                request.getMemo(),
                now);

        receptionRepository.save(reception);

        if (reservation != null) {
            reservation.markReceived(reception.getReceptionId(), now);
        }

        publishOutpatientReceptionRegistered(reception);
    }

    /**
     * 환자의 외래 진료 이력으로 초진/재진을 판정한다 — 이력이 없으면 초진, 있으면 재진.
     * 외래 서비스가 응답하지 않거나 오류이거나, 연동이 꺼져 있으면(빈 없음) 판정하지 않는다(determined=false).
     * 접수를 막지 않고, 재진으로 간주하지도 않는다 — 화면이 접수 담당자에게 직접 선택하게 한다.
     */
    @Override
    public VisitTypeResponsedto getVisitType(String patientId) {
        VisitTypeResponsedto undetermined = VisitTypeResponsedto.builder().determined(false).build();
        if (outpatientVisitHistoryBusinessDelegate.isEmpty()) {
            return undetermined;
        }
        try {
            OutpatientVisitHistoryResponsedto history =
                    outpatientVisitHistoryBusinessDelegate.get().getVisitHistory(patientId);
            if (history == null) {
                return undetermined;
            }
            return VisitTypeResponsedto.builder()
                    .visitType(history.isHasVisitRecord() ? "REVISIT" : "INITIAL")
                    .determined(true)
                    .lastVisitDate(history.getLastVisitDate())
                    .build();
        } catch (RestClientException e) {
            log.warn("외래 진료 이력 조회 실패 - 초진/재진을 판정하지 않는다. patientId={}", patientId, e);
            return undetermined;
        }
    }

    /**
     * 예약 등록 — 예약 목록에 쌓이고, 나중에 "접수하기"로 접수가 된다. (접수·외래 이벤트는 접수하기 때 발생)
     */
    @Override
    @Transactional
    public void registerReservation(ReservationRequestdto request) {

        if (request.getPatientId() == null || request.getPatientId().isBlank()) {
            throw new InvalidReservationException("환자는 필수입니다.");
        }
        if (request.getDeptId() == null || request.getDeptId().isBlank()) {
            throw new InvalidReservationException("진료과는 필수입니다.");
        }
        String doctorId = requireDoctorId(request.getDoctorId());
        if (!VISIT_TYPES.contains(request.getVisitType())) {
            throw new InvalidReservationException("초진/재진은 INITIAL/REVISIT만 가능합니다. visitType=" + request.getVisitType());
        }
        if (request.getReservationDate() == null || request.getReservationDate().isBefore(LocalDate.now())) {
            throw new InvalidReservationException("예약일은 오늘 이후여야 합니다. reservationDate=" + request.getReservationDate());
        }
        if (request.getReservationTime() == null || !RESERVATION_TIME_PATTERN.matcher(request.getReservationTime()).matches()) {
            throw new InvalidReservationException("예약 시간은 HH:mm 형식이어야 합니다. reservationTime=" + request.getReservationTime());
        }

        LocalDateTime now = LocalDateTime.now();

        reservationRepository.save(ReservationEntity.builder()
                .reservationId(UUID.randomUUID().toString())
                .patientId(request.getPatientId())
                .deptId(request.getDeptId())
                .doctorId(doctorId)
                .visitType(request.getVisitType())
                .reservationDate(request.getReservationDate())
                .reservationTime(request.getReservationTime())
                .memo(request.getMemo())
                .status(ReservationEntity.STATUS_RESERVED)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    /**
     * 예약 취소 — 접수되지 않은(RESERVED) 예약만 취소한다. 접수하기와 겹치지 않도록 예약 행을 잠그고 확인한다.
     * (접수·외래/응급 이벤트와는 무관: 예약만 등록된 상태에서는 아직 상대 서비스로 나간 게 없다)
     */
    @Override
    @Transactional
    public void cancelReservation(String reservationId) {
        ReservationEntity reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        if (!ReservationEntity.STATUS_RESERVED.equals(reservation.getStatus())) {
            throw new ReservationNotReceivableException(
                    reservationId, "예약됨(RESERVED) 상태가 아닙니다. status=" + reservation.getStatus());
        }
        reservation.cancel(LocalDateTime.now());
    }

    /**
     * 예약 목록 조회 (오늘 이후 예약, 최근 등록순)
     * - 진료과명(공통코드 캐시)/의사명(admin-service)을 함께 채운다. 환자명은 프론트가 CB2 batch 조회로 조합한다.
     */
    @Override
    public List<ReservationResponsedto> getReservationList() {
        List<ReservationEntity> reservations = reservationRepository
                .findByReservationDateGreaterThanEqualOrderByCreatedAtDesc(LocalDate.now());

        Map<String, String> deptNames = getDeptNames();
        Map<String, String> doctorNames = getDoctorNames();

        return reservations.stream()
                .map(r -> ReservationResponsedto.builder()
                        .reservationId(r.getReservationId())
                        .patientId(r.getPatientId())
                        .deptId(r.getDeptId())
                        .deptName(deptNames.get(r.getDeptId()))
                        .doctorId(r.getDoctorId())
                        .doctorName(doctorNames.get(r.getDoctorId()))
                        .visitType(r.getVisitType())
                        .reservationDate(r.getReservationDate())
                        .reservationTime(r.getReservationTime())
                        .memo(r.getMemo())
                        .status(r.getStatus())
                        .receptionId(r.getReceptionId())
                        .build())
                .toList();
    }

    /**
     * 외래 접수(응급이 아닌 모든 접수)인 경우, 트랜잭션 커밋 후 외래 서비스로 접수 등록 이벤트를
     * 발행하도록 예약한다. (실제 Kafka 전송은 {@code @TransactionalEventListener(AFTER_COMMIT)} 에서 수행)
     *
     * <p>이 메서드가 호출되는 {@link #registerReception}은 외래 접수 전용 경로다
     * (응급 접수는 {@link #registerEmergencyReception} 를 통하며 이 이벤트를 발행하지 않는다).
     * 접수유형은 예약/당일(RESERVATION/WALK_IN)로 들어오므로, 응급만 방어적으로 제외한다.
     */
    private void publishOutpatientReceptionRegistered(ReceptionEntity reception) {
        if (RECEPTION_TYPE_EMERGENCY.equals(reception.getReceptionType())) {
            return;
        }
        eventPublisher.publishEvent(ReceptionRegisteredInternalEvent.from(reception));
    }

    /**
     * 담당의(ADM EMPLOYEE.EMP_ID)를 검증한다. 외래·응급 공통.
     * RECEPTION.DOCTOR_ID 가 NOT NULL(length 36)이고, 외래(OPD)도 이벤트의 data.doctorId 를
     * DOCTOR_ID(NOT NULL, length 36)에 그대로 저장하므로 비어 있거나 36자를 넘는 값은 접수 단계에서 막는다.
     */
    private String requireDoctorId(String doctorId) {
        if (doctorId == null || doctorId.isBlank()) {
            throw new DoctorRequiredException("접수 시 담당의 선택이 필수입니다.");
        }
        String trimmed = doctorId.trim();
        if (trimmed.length() > DOCTOR_ID_MAX_LENGTH) {
            throw new DoctorRequiredException("담당의 ID는 " + DOCTOR_ID_MAX_LENGTH + "자 이내여야 합니다. doctorId=" + trimmed);
        }
        return trimmed;
    }

    /**
     * 응급접수 등록
     */
    @Override
    @Transactional
    public EmergencyReceptionResponsedto registerEmergencyReception(EmergencyReceptionRequestdto request) {

        // KTAS 는 접수 시점에 분류 전일 수 있어 선택값(null 허용). 주호소만 필수.
        if (request.getChiefComplaint() == null) {
            throw new EmergencyInfoRequiredException();
        }

        // 응급 서비스로 넘기는 목록에 환자명을 함께 담는다.
        // CB2 장애로 응급접수 자체가 막히면 안 되므로, 조회 실패 시 환자명 없이 진행한다.
        String patientName = resolvePatientName(request.getPatientId());

        String doctorId = requireDoctorId(request.getDoctorId());

        LocalDateTime now = LocalDateTime.now();

        ReceptionEntity reception = createReceptionEntity(
                request.getPatientId(),
                request.getDeptId(),
                doctorId,
                RECEPTION_TYPE_EMERGENCY,
                null, // 응급은 초진/재진 구분 없음
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
        eventPublisher.publishEvent(EmergencyReceptionRegisteredInternalEvent.from(reception, emergencyInfo));

        return toEmergencyResponseDto(reception, emergencyInfo, patientName,
                getDeptNames().get(reception.getDeptId()),
                getDoctorNames().get(doctorId));
    }

    /**
     * 응급접수 목록 조회 (응급접수홈 전용)
     * - 당일(00:00~익일 00:00) 접수된 응급 건 전체(취소 포함), 접수일시 최신순.
     *   외래 목록과 같이 취소 건도 내려주고, 화면에서 상태 표시/취소버튼 비활성화로 구분한다.
     * - EMERGENCY_INFO(KTAS·내원경로·주호소 등)를 조인하고, 진료과명/의사명/환자명을 서버에서 채운다.
     */
    @Override
    public List<EmergencyReceptionResponsedto> getEmergencyReceptionList() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        List<ReceptionEntity> receptions = receptionRepository
                .findByReceptionTypeAndReceptionDateBetween(RECEPTION_TYPE_EMERGENCY, startOfDay, startOfDay.plusDays(1))
                .stream()
                .sorted(Comparator.comparing(ReceptionEntity::getReceptionDate).reversed())
                .toList();

        if (receptions.isEmpty()) {
            return List.of();
        }

        List<String> receptionIds = receptions.stream().map(ReceptionEntity::getReceptionId).toList();
        Map<String, EmergencyInfoEntity> emergencyInfos = emergencyInfoRepository.findByReceptionIdIn(receptionIds).stream()
                .collect(Collectors.toMap(EmergencyInfoEntity::getReceptionId, info -> info, (a, b) -> a));

        Map<String, String> deptNames = getDeptNames();
        Map<String, String> doctorNames = getDoctorNames();
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
     * 응급접수 등록 직전 사전 확인 — 같은 환자의 진행 중(미퇴실) 응급접수가 있는지 조회한다.
     * 등록 트랜잭션과는 완전히 분리된 읽기 전용 호출이다 (사용자가 경고를 보고 취소하면 아무것도 생성되면 안 되므로).
     * 응급 서비스 장애/타임아웃, 또는 기능 자체가 꺼져있는 경우(빈 없음) 모두 경고 없이 진행하도록 fail-open 한다.
     */
    @Override
    public EmergencyActiveCheckResponsedto checkActiveEmergencyReception(String patientId) {
        if (emergencyActiveCheckBusinessDelegate.isEmpty()) {
            return EmergencyActiveCheckResponsedto.builder()
                    .hasActiveReception(false)
                    .activeReceptions(List.of())
                    .build();
        }
        try {
            List<ActiveEmergencyReceptionResponsedto> active =
                    emergencyActiveCheckBusinessDelegate.get().getActiveReceptions(patientId);
            return EmergencyActiveCheckResponsedto.builder()
                    .hasActiveReception(!active.isEmpty())
                    .activeReceptions(active)
                    .build();
        } catch (RestClientException e) {
            log.warn("응급 서비스 중복접수 확인 실패 - 경고 없이 진행(fail-open). patientId={}", patientId, e);
            return EmergencyActiveCheckResponsedto.builder()
                    .hasActiveReception(false)
                    .activeReceptions(List.of())
                    .build();
        }
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
            String patientId, String deptId, String doctorId, String receptionType, String visitType,
            String memo, LocalDateTime now) {

        return ReceptionEntity.builder()
                .receptionId(UUID.randomUUID().toString())
                .patientId(patientId)
                .deptId(deptId)
                .doctorId(doctorId)
                .receptionNo(generateReceptionNo(now))
                .receptionType(receptionType)
                .visitType(visitType)
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
        Map<String, String> doctorNames = getDoctorNames();

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
     * 의사ID → 의사명
     * 담당의는 ADM 직원(EMPLOYEE.EMP_ID)으로 저장되므로 admin-service 직원 목록을 1회 조회해 맵을 만든다.
     * admin-service가 응답하지 않아도 접수 목록 자체는 볼 수 있어야 하므로, 실패하면 빈 맵을 반환한다.
     * (예전 의사 ID로 저장된 기존 데이터는 직원 목록에 없으므로 의사명이 비어 보인다)
     */
    private Map<String, String> getDoctorNames() {
        try {
            ApiResponse<List<EmpResponsedto>> response = empBusinessDelegate.getEmps();
            if (response == null || response.data() == null) {
                return Map.of();
            }
            Map<String, String> doctorNames = new HashMap<>();
            for (EmpResponsedto emp : response.data()) {
                if (emp.getEmpId() != null) {
                    doctorNames.put(emp.getEmpId(), emp.getEmpName());
                }
            }
            return doctorNames;
        } catch (RestClientException e) {
            log.warn("admin-service 직원 목록 조회 실패 - 의사명 없이 접수 목록 반환 ({})", e.getMessage());
            return Map.of();
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
        String doctorName = getDoctorNames().get(reception.getDoctorId());

        return toDetailResponseDto(reception, deptName, doctorName);
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

        // 취소는 취소 API로만 — 여기로 바꾸면 취소 기록과 외래/응급 취소 이벤트가 남지 않는다.
        if (STATUS_CANCELLED.equals(request.getNewStatus())) {
            throw new CancelViaStatusChangeNotAllowedException(receptionId);
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

        if (RECEPTION_TYPE_EMERGENCY.equals(reception.getReceptionType())) {
            checkEmergencyCancellable(receptionId);
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

        publishReceptionCancelled(reception, now);
    }

    /**
     * 응급 접수 취소 전, 응급에 진료 기록이 있는지 확인한다.
     * 응급은 진료 기록이 있는 접수의 취소 이벤트를 받아도 취소하지 않으므로 여기서 먼저 막는다.
     * 확인하지 못하면(응급 장애·타임아웃, CANNOT_VERIFY) 취소를 막는다 (fail-closed).
     * 기능이 꺼져 있으면(응급 배포 전, 빈 없음) 확인 없이 진행한다.
     *
     * <p>이 조회는 사전 안내용이다. 조회와 실제 취소 사이에 응급에 기록이 생길 수 있어,
     * 최종 판단은 응급이 취소 이벤트를 받을 때 다시 한다.
     */
    private void checkEmergencyCancellable(String receptionId) {
        if (emergencyCancelCheckBusinessDelegate.isEmpty()) {
            return;
        }
        EmergencyCancellableResponsedto result;
        try {
            result = emergencyCancelCheckBusinessDelegate.get().getCancellable(receptionId);
        } catch (RestClientException e) {
            log.warn("응급 취소 가능 여부 조회 실패 - 취소를 막는다(fail-closed). receptionId={}", receptionId, e);
            throw new EmergencyCancelCheckFailedException(receptionId, e.getMessage());
        }
        if (result == null) {
            throw new EmergencyCancelCheckFailedException(receptionId, "응답 없음");
        }
        if (result.isCancellable()) {
            return;
        }
        if (EMERGENCY_CANCEL_HAS_RECORDS.equals(result.getReasonCode())) {
            throw new EmergencyCancelNotAllowedException(receptionId, result.getRecords());
        }
        // CANNOT_VERIFY 및 그 밖의 취소 불가 응답 — 잠시 뒤 다시 시도하게 한다.
        throw new EmergencyCancelCheckFailedException(receptionId, result.getReasonCode());
    }

    /**
     * 트랜잭션 커밋 후 접수 취소 이벤트를 발행하도록 예약한다.
     * - 외래: 외래 토픽으로 eventType=ReceptionCancelled (등록과 같은 receptionId, 나머지 필드도 등록 때 값)
     * - 응급: 응급 토픽으로 같은 방식의 취소 이벤트 (응급 서비스 반영 전까지는 설정으로 꺼져 있음)
     */
    private void publishReceptionCancelled(ReceptionEntity reception, LocalDateTime cancelledAt) {
        if (!RECEPTION_TYPE_EMERGENCY.equals(reception.getReceptionType())) {
            eventPublisher.publishEvent(ReceptionCancelledInternalEvent.from(reception, cancelledAt));
            return;
        }
        // 등록 이벤트와 같은 값을 채우기 위해 응급접수정보(내원경로·주호소)를 다시 읽는다.
        EmergencyInfoEntity emergencyInfo = emergencyInfoRepository
                .findByReceptionIdIn(List.of(reception.getReceptionId()))
                .stream().findFirst().orElse(null);
        eventPublisher.publishEvent(new EmergencyReceptionCancelledInternalEvent(
                EmergencyReceptionRegisteredInternalEvent.from(reception, emergencyInfo),
                cancelledAt));
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
                .visitType(reception.getVisitType())
                .status(reception.getStatus())
                .receptionDate(reception.getReceptionDate())
                .build();
    }

    private ReceptionDetailResponsedto toDetailResponseDto(
            ReceptionEntity reception, String deptName, String doctorName) {
        return ReceptionDetailResponsedto.builder()
                .receptionId(reception.getReceptionId())
                .patientId(reception.getPatientId())
                .deptId(reception.getDeptId())
                .deptName(deptName)
                .doctorId(reception.getDoctorId())
                .doctorName(doctorName)
                .receptionNo(reception.getReceptionNo())
                .receptionType(reception.getReceptionType())
                .visitType(reception.getVisitType())
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

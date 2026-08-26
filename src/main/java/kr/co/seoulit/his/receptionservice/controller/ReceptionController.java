package kr.co.seoulit.his.receptionservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import kr.co.seoulit.his.receptionservice.common.ApiResponse;
import kr.co.seoulit.his.receptionservice.dto.request.EmergencyReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionCancelRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionRequestdto;
import kr.co.seoulit.his.receptionservice.dto.request.ReceptionStatusChangeRequestdto;
import kr.co.seoulit.his.receptionservice.dto.response.DepartmentResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.DoctorResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.EmergencyReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.PatientDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.PatientSummaryResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReceptionDetailResponsedto;
import kr.co.seoulit.his.receptionservice.dto.response.ReceptionResponsedto;
import kr.co.seoulit.his.receptionservice.service.PatientService;
import kr.co.seoulit.his.receptionservice.service.ReceptionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reception")
@RequiredArgsConstructor
public class ReceptionController {

        private final ReceptionService receptionService;
        private final PatientService patientService;

        /**
         * 접수 등록
         */
        @PostMapping
        public ResponseEntity<ApiResponse<Void>> registerReception(
                        @RequestBody ReceptionRequestdto request) {

                receptionService.registerReception(request);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(HttpStatus.CREATED.value(), null));
        }

        /**
         * 접수 목록 조회
         */
        @GetMapping
        public ResponseEntity<ApiResponse<List<ReceptionResponsedto>>> getReceptionList() {

                return ResponseEntity.ok(
                                ApiResponse.success(HttpStatus.OK.value(), receptionService.getReceptionList()));
        }

        /**
         * 접수 상세 조회
         */
        @GetMapping("/{receptionId}")
        public ResponseEntity<ApiResponse<ReceptionDetailResponsedto>> getReception(
                        @PathVariable Long receptionId) {

                return ResponseEntity.ok(
                                ApiResponse.success(HttpStatus.OK.value(), receptionService.getReception(receptionId)));
        }

        /**
         * 응급접수 등록
         */
        @PostMapping("/emergency")
        public ResponseEntity<EmergencyReceptionResponsedto> registerEmergencyReception(
                        @RequestBody EmergencyReceptionRequestdto request) {

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(receptionService.registerEmergencyReception(request));
        }

        /**
         * 접수 상태 변경
         */
        @PatchMapping("/{receptionId}/status")
        public ResponseEntity<String> changeReceptionStatus(
                        @PathVariable Long receptionId,
                        @RequestBody ReceptionStatusChangeRequestdto request) {

                receptionService.changeReceptionStatus(receptionId, request);

                return ResponseEntity.ok("접수 상태가 변경되었습니다.");
        }

        /**
         * 접수 취소
         */
        @PatchMapping("/{receptionId}/cancel")
        public ResponseEntity<ApiResponse<Void>> cancelReception(
                        @PathVariable Long receptionId,
                        @RequestBody ReceptionCancelRequestdto request) {

                receptionService.cancelReception(receptionId, request);

                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), null));
        }

        /**
         * CB2 환자 목록 조회
         */
        @GetMapping("/patients")
        public ResponseEntity<List<PatientSummaryResponsedto>> getPatientList() {

                return ResponseEntity.ok(
                                patientService.getPatientList());
        }

        /**
         * CB2 환자 상세 조회
         */
        @GetMapping("/patients/{patientId}")
        public ResponseEntity<PatientDetailResponsedto> getPatient(
                        @PathVariable String patientId) {

                return ResponseEntity.ok(
                                patientService.getPatient(patientId));
        }

        /**
         * 진료과 목록 조회
         */
        @GetMapping("/departments")
        public ResponseEntity<ApiResponse<List<DepartmentResponsedto>>> getDepartments() {

                return ResponseEntity.ok(
                                receptionService.getDepartments());
        }

        /**
         * 진료과별 의사 목록 조회
         */
        @GetMapping("/departments/{deptId}/doctors")
        public ResponseEntity<ApiResponse<List<DoctorResponsedto>>> getDoctorsByDepartment(
                        @PathVariable Long deptId) {

                return ResponseEntity.ok(
                                receptionService.getDoctorsByDepartment(deptId));
        }

}
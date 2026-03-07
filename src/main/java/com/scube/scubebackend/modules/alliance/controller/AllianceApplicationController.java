package com.scube.scubebackend.modules.alliance.controller;

import com.scube.scubebackend.common.controller.BaseController;
import com.scube.scubebackend.common.model.dto.BaseResponse;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationCreateRequest;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationCreateResponse;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationListResponse;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationReviewRequest;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationUpdateRequest;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationVO;
import com.scube.scubebackend.modules.alliance.service.AllianceApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alliance")
public class AllianceApplicationController extends BaseController {

    @Autowired
    private AllianceApplicationService allianceApplicationService;

    @PostMapping("/applications")
    public BaseResponse<AllianceApplicationCreateResponse> createApplication(
            @RequestBody AllianceApplicationCreateRequest request) {
        AllianceApplicationCreateResponse response = allianceApplicationService.createApplication(request);
        return BaseResponse.success(response);
    }

    @GetMapping("/applications/mine")
    public BaseResponse<AllianceApplicationListResponse> getMyApplications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        List<AllianceApplicationVO> all = allianceApplicationService.getMyApplications();
        List<AllianceApplicationVO> filtered = all.stream()
                .filter(item -> status == null || status.isBlank() || status.equalsIgnoreCase(item.getStatus()))
                .toList();

        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;
        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());

        AllianceApplicationListResponse response = new AllianceApplicationListResponse();
        response.setTotal(filtered.size());
        response.setItems(filtered.subList(fromIndex, toIndex));
        return BaseResponse.success(response);
    }

    @GetMapping("/applications/{id}")
    public BaseResponse<AllianceApplicationVO> getMyApplication(@PathVariable Long id) {
        return BaseResponse.success(allianceApplicationService.getMyApplicationById(id));
    }

    @PutMapping("/applications/{id}")
    public BaseResponse<AllianceApplicationVO> updateMyApplication(
            @PathVariable Long id,
            @RequestBody AllianceApplicationUpdateRequest request) {
        return BaseResponse.success(allianceApplicationService.updateMyApplication(id, request));
    }

    @DeleteMapping("/applications/{id}")
    public BaseResponse<Boolean> deleteMyApplication(@PathVariable Long id) {
        allianceApplicationService.deleteMyApplication(id);
        return BaseResponse.success(true);
    }

    @GetMapping("/admin/applications")
    public BaseResponse<AllianceApplicationListResponse> getAllApplications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        List<AllianceApplicationVO> items = allianceApplicationService.getAllApplications(status, kind, page, pageSize);
        if (keyword != null && !keyword.isBlank()) {
            String keywordLower = keyword.toLowerCase();
            items = items.stream().filter(item -> matchesKeyword(item, keywordLower)).toList();
        }

        AllianceApplicationListResponse response = new AllianceApplicationListResponse();
        response.setTotal(items.size());
        response.setItems(items);
        return BaseResponse.success(response);
    }

    @PostMapping("/admin/applications/{id}/audit")
    public BaseResponse<AllianceApplicationVO> reviewApplication(
            @PathVariable Long id,
            @RequestBody AllianceApplicationReviewRequest request) {
        allianceApplicationService.reviewApplication(id, request);
        AllianceApplicationVO updated = allianceApplicationService.getApplicationById(id);
        return BaseResponse.success(updated);
    }

    private boolean matchesKeyword(AllianceApplicationVO item, String keywordLower) {
        return containsIgnoreCase(item.getRealName(), keywordLower)
                || containsIgnoreCase(item.getOrgName(), keywordLower)
                || containsIgnoreCase(item.getPhone(), keywordLower)
                || containsIgnoreCase(item.getContactMethod(), keywordLower)
                || containsIgnoreCase(item.getUserDisplayId(), keywordLower);
    }

    private boolean containsIgnoreCase(String value, String keywordLower) {
        return value != null && value.toLowerCase().contains(keywordLower);
    }
}


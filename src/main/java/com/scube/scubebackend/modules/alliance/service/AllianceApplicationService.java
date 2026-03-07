package com.scube.scubebackend.modules.alliance.service;

import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationCreateRequest;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationCreateResponse;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationReviewRequest;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationUpdateRequest;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationVO;

import java.util.List;

public interface AllianceApplicationService {
    AllianceApplicationCreateResponse createApplication(AllianceApplicationCreateRequest request);
    List<AllianceApplicationVO> getMyApplications();
    AllianceApplicationVO getMyApplicationById(Long id);
    AllianceApplicationVO updateMyApplication(Long id, AllianceApplicationUpdateRequest request);
    void deleteMyApplication(Long id);

    List<AllianceApplicationVO> getAllApplications(String status, String kind, Integer page, Integer pageSize);
    AllianceApplicationVO getApplicationById(Long id);
    void reviewApplication(Long id, AllianceApplicationReviewRequest request);
}


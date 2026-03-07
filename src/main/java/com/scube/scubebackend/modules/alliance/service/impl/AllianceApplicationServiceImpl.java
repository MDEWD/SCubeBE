package com.scube.scubebackend.modules.alliance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.modules.alliance.mapper.AllianceApplicationMapper;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationCreateRequest;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationCreateResponse;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationReviewRequest;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationUpdateRequest;
import com.scube.scubebackend.modules.alliance.model.dto.AllianceApplicationVO;
import com.scube.scubebackend.modules.alliance.model.entity.AllianceApplication;
import com.scube.scubebackend.modules.alliance.service.AllianceApplicationService;
import com.scube.scubebackend.modules.user.mapper.UserMapper;
import com.scube.scubebackend.modules.user.model.entity.User;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.util.UserContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AllianceApplicationServiceImpl implements AllianceApplicationService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_PARTNER = "PARTNER";

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";

    private static final String KIND_PERSON = "person";
    private static final String KIND_COMPANY = "company";

    @Autowired
    private AllianceApplicationMapper allianceApplicationMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AllianceApplicationCreateResponse createApplication(AllianceApplicationCreateRequest request) {
        LoginUser loginUser = requireLogin();
        validateCreateRequest(request);

        AllianceApplication application = new AllianceApplication();
        BeanUtils.copyProperties(request, application);
        application.setUserId(loginUser.getId());
        application.setUserDisplayId(resolveDisplayId(loginUser.getId()));
        application.setStatus(STATUS_PENDING);
        application.setRejectReason(null);
        application.setReviewTime(null);
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        application.setIsDelete(0);

        allianceApplicationMapper.insert(application);

        AllianceApplicationCreateResponse response = new AllianceApplicationCreateResponse();
        response.setId(application.getId());
        response.setStatus(application.getStatus());
        return response;
    }

    @Override
    public List<AllianceApplicationVO> getMyApplications() {
        LoginUser loginUser = requireLogin();
        LambdaQueryWrapper<AllianceApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AllianceApplication::getUserId, loginUser.getId())
                .eq(AllianceApplication::getIsDelete, 0)
                .orderByDesc(AllianceApplication::getCreateTime);

        List<AllianceApplication> items = allianceApplicationMapper.selectList(queryWrapper);
        return items.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AllianceApplicationVO getMyApplicationById(Long id) {
        LoginUser loginUser = requireLogin();
        AllianceApplication application = allianceApplicationMapper.selectById(id);
        if (application == null || application.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "申请不存在");
        }
        if (!Objects.equals(application.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看");
        }
        return toVO(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AllianceApplicationVO updateMyApplication(Long id, AllianceApplicationUpdateRequest request) {
        LoginUser loginUser = requireLogin();
        validateUpdateRequest(request);

        AllianceApplication application = allianceApplicationMapper.selectById(id);
        if (application == null || application.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "申请不存在");
        }
        if (!Objects.equals(application.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改");
        }
        if (!STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅可修改待审核申请");
        }

        BeanUtils.copyProperties(request, application);
        application.setUpdateTime(LocalDateTime.now());
        allianceApplicationMapper.updateById(application);
        return toVO(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyApplication(Long id) {
        LoginUser loginUser = requireLogin();
        AllianceApplication application = allianceApplicationMapper.selectById(id);
        if (application == null || application.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "申请不存在");
        }
        if (!Objects.equals(application.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除");
        }
        if (!STATUS_PENDING.equals(application.getStatus()) && !STATUS_REJECTED.equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅可删除待审核或已驳回申请");
        }
        application.setIsDelete(1);
        application.setUpdateTime(LocalDateTime.now());
        allianceApplicationMapper.updateById(application);
    }

    @Override
    public List<AllianceApplicationVO> getAllApplications(String status, String kind, Integer page, Integer pageSize) {
        LoginUser loginUser = requireLogin();
        if (!ROLE_ADMIN.equalsIgnoreCase(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;

        LambdaQueryWrapper<AllianceApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AllianceApplication::getIsDelete, 0);
        if (status != null && !status.isBlank()) {
            queryWrapper.eq(AllianceApplication::getStatus, status);
        }
        if (kind != null && !kind.isBlank()) {
            queryWrapper.eq(AllianceApplication::getKind, kind);
        }
        queryWrapper.orderByDesc(AllianceApplication::getCreateTime);

        Page<AllianceApplication> pageResult = allianceApplicationMapper.selectPage(
                new Page<>(safePage, safePageSize), queryWrapper);

        return pageResult.getRecords().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AllianceApplicationVO getApplicationById(Long id) {
        LoginUser loginUser = requireLogin();
        if (!ROLE_ADMIN.equalsIgnoreCase(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        AllianceApplication application = allianceApplicationMapper.selectById(id);
        if (application == null || application.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "申请不存在");
        }
        return toVO(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(Long id, AllianceApplicationReviewRequest request) {
        LoginUser loginUser = requireLogin();
        if (!ROLE_ADMIN.equalsIgnoreCase(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核状态不能为空");
        }

        AllianceApplication application = allianceApplicationMapper.selectById(id);
        if (application == null || application.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "申请不存在");
        }

        String status = request.getStatus().toLowerCase();
        if (!STATUS_APPROVED.equals(status) && !STATUS_REJECTED.equals(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核状态不合法");
        }

        if (!STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "申请已审核");
        }

        application.setStatus(status);
        application.setRejectReason(STATUS_REJECTED.equals(status) ? request.getRejectReason() : null);
        application.setReviewTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        allianceApplicationMapper.updateById(application);

        if (STATUS_APPROVED.equals(status)) {
            User user = userMapper.selectById(application.getUserId());
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            }
            user.setUserRole(ROLE_PARTNER);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    private LoginUser requireLogin() {
        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return loginUser;
    }

    private void validateCreateRequest(AllianceApplicationCreateRequest request) {
        if (request == null || request.getKind() == null || request.getKind().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "kind不能为空");
        }
        validateKind(request.getKind());
        validateFieldsByKind(request.getKind(), request.getRealName(), request.getPhone(), request.getIdNumber(),
                request.getIdFrontImage(), request.getIdBackImage(), request.getOrgName(), request.getCreditCode(),
                request.getLicenseImage(), request.getJob(), request.getMainBusiness(), request.getContactName(),
                request.getContactMethod());
    }

    private void validateUpdateRequest(AllianceApplicationUpdateRequest request) {
        if (request == null || request.getKind() == null || request.getKind().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "kind不能为空");
        }
        validateKind(request.getKind());
        validateFieldsByKind(request.getKind(), request.getRealName(), request.getPhone(), request.getIdNumber(),
                request.getIdFrontImage(), request.getIdBackImage(), request.getOrgName(), request.getCreditCode(),
                request.getLicenseImage(), request.getJob(), request.getMainBusiness(), request.getContactName(),
                request.getContactMethod());
    }

    private void validateKind(String kind) {
        String normalized = kind.toLowerCase();
        if (!KIND_PERSON.equals(normalized) && !KIND_COMPANY.equals(normalized)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "kind必须为person或company");
        }
    }

    private void validateFieldsByKind(String kind,
                                      String realName,
                                      String phone,
                                      String idNumber,
                                      String idFrontImage,
                                      String idBackImage,
                                      String orgName,
                                      String creditCode,
                                      String licenseImage,
                                      String job,
                                      String mainBusiness,
                                      String contactName,
                                      String contactMethod) {
        String normalized = kind.toLowerCase();
        if (KIND_PERSON.equals(normalized)) {
            requireNotBlank(realName, "realName不能为空");
            requireNotBlank(phone, "phone不能为空");
            requireNotBlank(idNumber, "idNumber不能为空");
            requireNotBlank(idFrontImage, "idFrontImage不能为空");
            requireNotBlank(idBackImage, "idBackImage不能为空");
        } else {
            requireNotBlank(orgName, "orgName不能为空");
            requireNotBlank(creditCode, "creditCode不能为空");
            requireNotBlank(licenseImage, "licenseImage不能为空");
            requireNotBlank(job, "job不能为空");
            requireNotBlank(mainBusiness, "mainBusiness不能为空");
            requireNotBlank(contactName, "contactName不能为空");
            requireNotBlank(contactMethod, "contactMethod不能为空");
        }
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, message);
        }
    }

    private String resolveDisplayId(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return user.getDisplayId();
    }

    private AllianceApplicationVO toVO(AllianceApplication application) {
        AllianceApplicationVO vo = new AllianceApplicationVO();
        BeanUtils.copyProperties(application, vo);
        return vo;
    }
}


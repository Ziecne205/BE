package com.parking.modules.admin;

import com.parking.common.exception.BadRequestException;
import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.AuditLogWriter;
import com.parking.entity.SystemConfig;
import com.parking.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final AuditLogWriter auditLogService;

    public List<SystemConfig> findAll() {
        return systemConfigRepository.findAll();
    }

    public SystemConfig findByKey(String key) {
        return systemConfigRepository.findById(Objects.requireNonNull(key))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cau hinh: " + key));
    }

    @Transactional
    public SystemConfig create(SystemConfigRequest request, String actorUsername) {
        if (request.getConfigKey() == null || request.getConfigKey().isBlank()) {
            throw new BadRequestException("Config key khong duoc trong", "VALIDATION_ERROR");
        }
        if (systemConfigRepository.existsById(request.getConfigKey())) {
            throw new BusinessRuleException("Config key '" + request.getConfigKey() + "' da ton tai");
        }
        SystemConfig config = SystemConfig.builder()
                .configKey(request.getConfigKey())
                .configValue(request.getConfigValue())
                .description(request.getDescription())
                .build();
        SystemConfig saved = systemConfigRepository.save(config);
        auditLogService.log(actorUsername, "ADMIN_CREATE_SYSTEM_CONFIG", "SystemConfig", saved.getConfigKey(),
                "Tao cau hinh " + saved.getConfigKey() + " = " + saved.getConfigValue());
        return saved;
    }

    @Transactional
    public SystemConfig update(String key, SystemConfigRequest request, String actorUsername) {
        SystemConfig config = findByKey(key);
        String oldValue = config.getConfigValue();
        config.setConfigValue(request.getConfigValue());
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }
        SystemConfig saved = systemConfigRepository.save(config);
        auditLogService.log(actorUsername, "ADMIN_UPDATE_SYSTEM_CONFIG", "SystemConfig", key,
                "Doi " + key + ": '" + oldValue + "' -> '" + saved.getConfigValue() + "'");
        return saved;
    }

    @Transactional
    public void delete(String key, String actorUsername) {
        findByKey(key);
        systemConfigRepository.deleteById(key);
        auditLogService.log(actorUsername, "ADMIN_DELETE_SYSTEM_CONFIG", "SystemConfig", key,
                "Xoa cau hinh " + key);
    }
}

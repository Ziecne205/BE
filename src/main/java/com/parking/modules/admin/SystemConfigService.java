package com.parking.modules.admin;

import com.parking.common.exception.ResourceNotFoundException;
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

    public List<SystemConfig> findAll() {
        return systemConfigRepository.findAll();
    }

    public SystemConfig findByKey(String key) {
        return systemConfigRepository.findById(Objects.requireNonNull(key))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cau hinh: " + key));
    }

    @Transactional
    public SystemConfig create(SystemConfigRequest request) {
        if (systemConfigRepository.existsById(request.getConfigKey())) {
            throw new RuntimeException("Config key '" + request.getConfigKey() + "' da ton tai");
        }
        SystemConfig config = SystemConfig.builder()
                .configKey(request.getConfigKey())
                .configValue(request.getConfigValue())
                .description(request.getDescription())
                .build();
        return systemConfigRepository.save(config);
    }

    @Transactional
    public SystemConfig update(String key, SystemConfigRequest request) {
        SystemConfig config = findByKey(key);
        config.setConfigValue(request.getConfigValue());
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }
        return systemConfigRepository.save(config);
    }

    @Transactional
    public void delete(String key) {
        findByKey(key);
        systemConfigRepository.deleteById(key);
    }
}

package com.visionfund.trrs.repository;

import com.visionfund.trrs.domain.SystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemRepository extends JpaRepository<SystemEntity, Integer> {
    Optional<SystemEntity> findBySystemCode(String systemCode);
}

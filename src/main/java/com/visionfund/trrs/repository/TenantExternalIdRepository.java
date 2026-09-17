package com.visionfund.trrs.repository;

import com.visionfund.trrs.domain.TenantExternalId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantExternalIdRepository extends JpaRepository<TenantExternalId, Long> {
    Optional<TenantExternalId> findBySourceSystemIdAndExternalTenantRefAndActiveTrue(
            Integer sourceSystemId, String externalTenantRef);
}

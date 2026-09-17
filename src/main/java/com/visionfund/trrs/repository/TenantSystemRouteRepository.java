package com.visionfund.trrs.repository;

import com.visionfund.trrs.domain.TenantSystemRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantSystemRouteRepository extends JpaRepository<TenantSystemRoute, Long> {
    Optional<TenantSystemRoute> findByTenantIdAndSourceSystemIdAndDestinationSystemIdAndActiveTrue(
            Integer tenantId, Integer sourceSystemId, Integer destinationSystemId);
}

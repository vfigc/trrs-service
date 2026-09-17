package com.visionfund.trrs.service;

import com.visionfund.trrs.domain.MfiMaster;
import com.visionfund.trrs.domain.SystemEntity;
import com.visionfund.trrs.domain.TenantExternalId;
import com.visionfund.trrs.domain.TenantSystemRoute;
import com.visionfund.trrs.repository.MfiMasterRepository;
import com.visionfund.trrs.repository.SystemRepository;
import com.visionfund.trrs.repository.TenantExternalIdRepository;
import com.visionfund.trrs.repository.TenantSystemRouteRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RouteResolutionService {

    private final MfiMasterRepository mfiMasterRepository;
    private final SystemRepository systemRepository;
    private final TenantSystemRouteRepository routeRepository;
    private final TenantExternalIdRepository externalIdRepository;

    public RouteResolutionService(MfiMasterRepository mfiMasterRepository,
                                   SystemRepository systemRepository,
                                   TenantSystemRouteRepository routeRepository,
                                   TenantExternalIdRepository externalIdRepository) {
        this.mfiMasterRepository = mfiMasterRepository;
        this.systemRepository = systemRepository;
        this.routeRepository = routeRepository;
        this.externalIdRepository = externalIdRepository;
    }

    public Optional<TenantExternalId> resolveTenantByExternalRef(String sourceSystemCode, String externalTenantRef) {
        return systemRepository.findBySystemCode(sourceSystemCode)
                .flatMap(source -> externalIdRepository.findBySourceSystemIdAndExternalTenantRefAndActiveTrue(
                        source.getSystemId(), externalTenantRef));
    }

    /**
     * destinationType is a category (CBS/UDM), not a specific instance - the
     * tenant's country determines which specific CBS system applies (e.g.
     * Zambia -> T24_ZM_R23), mirroring how the source schema's own
     * resolution-flow narrative describes this step.
     */
    public Optional<TenantSystemRoute> resolveRoute(Integer tenantId, String sourceSystemCode, String destinationType) {
        Optional<MfiMaster> tenant = mfiMasterRepository.findById(tenantId);
        Optional<SystemEntity> source = systemRepository.findBySystemCode(sourceSystemCode);
        if (tenant.isEmpty() || source.isEmpty()) {
            return Optional.empty();
        }

        String countryCode = tenant.get().getCountryCode();

        Optional<SystemEntity> destination = systemRepository.findAll().stream()
                .filter(SystemEntity::isActive)
                .filter(s -> destinationType.equalsIgnoreCase(s.getSystemType()))
                .filter(s -> s.getCountryCode() == null || s.getCountryCode().equalsIgnoreCase(countryCode))
                .findFirst();
        if (destination.isEmpty()) {
            return Optional.empty();
        }

        return routeRepository.findByTenantIdAndSourceSystemIdAndDestinationSystemIdAndActiveTrue(
                tenantId, source.get().getSystemId(), destination.get().getSystemId());
    }

    public Optional<String> systemCodeFor(Integer systemId) {
        return systemRepository.findById(systemId).map(SystemEntity::getSystemCode);
    }
}

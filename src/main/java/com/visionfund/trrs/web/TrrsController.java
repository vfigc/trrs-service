package com.visionfund.trrs.web;
import com.visionfund.trrs.domain.TenantExternalId;
import com.visionfund.trrs.repository.MfiMasterRepository;
import com.visionfund.trrs.service.RouteResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TrrsController {

    private final RouteResolutionService resolutionService;
    private final MfiMasterRepository mfiMasterRepository;

    public TrrsController(RouteResolutionService resolutionService, MfiMasterRepository mfiMasterRepository) {
        this.resolutionService = resolutionService;
        this.mfiMasterRepository = mfiMasterRepository;
    }

    // Resolution key is (source_system_id, external_tenant_ref) only - never country code.
    @GetMapping("/tenant-external-id")
    public ResponseEntity<TenantResolutionResponse> resolveTenant(
            @RequestParam String sourceSystemCode,
            @RequestParam String externalTenantRef) {
        return resolutionService.resolveTenantByExternalRef(sourceSystemCode, externalTenantRef)
                .map(TenantExternalId::getTenantId)
                .flatMap(mfiMasterRepository::findById)
                .map(mfi -> ResponseEntity.ok(
                        new TenantResolutionResponse(mfi.getTenantId(), mfi.getMfiName(), mfi.getCountryCode())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/routes")
    public ResponseEntity<RouteResolutionResponse> resolveRoute(
            @RequestParam Integer tenantId,
            @RequestParam String sourceSystemCode,
            @RequestParam String destinationType) {
        return resolutionService.resolveRoute(tenantId, sourceSystemCode, destinationType)
                .map(route -> {
                    String destCode = resolutionService.systemCodeFor(route.getDestinationSystemId()).orElse(null);
                    return ResponseEntity.ok(new RouteResolutionResponse(
                            route.getTenantId(), sourceSystemCode, destCode,
                            route.getAuthMethod(), route.getSecretsPath(),
                            route.getCbsAdapter(), route.getEndpoints(),
                            route.getS3BucketUrl(), route.getS3BucketName(),
                            route.getDwEndpoint(), route.getDwDatabaseName()));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

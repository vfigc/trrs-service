package com.visionfund.trrs.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tenant_external_id",
       uniqueConstraints = @UniqueConstraint(name = "uq_external_tenant_ref",
               columnNames = {"source_system_id", "external_tenant_ref"}))
public class TenantExternalId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Integer tenantId;

    @Column(name = "source_system_id", nullable = false)
    private Integer sourceSystemId;

    @Column(name = "external_tenant_ref", nullable = false, length = 100)
    private String externalTenantRef;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    protected TenantExternalId() {}

    public TenantExternalId(Integer tenantId, Integer sourceSystemId, String externalTenantRef,
                             boolean active, Instant updatedAt, String updatedBy) {
        this.tenantId = tenantId;
        this.sourceSystemId = sourceSystemId;
        this.externalTenantRef = externalTenantRef;
        this.active = active;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getId() { return id; }
    public Integer getTenantId() { return tenantId; }
    public Integer getSourceSystemId() { return sourceSystemId; }
    public String getExternalTenantRef() { return externalTenantRef; }
    public boolean isActive() { return active; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}

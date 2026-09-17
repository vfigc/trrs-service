package com.visionfund.trrs.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tenant_system_route",
       uniqueConstraints = @UniqueConstraint(name = "uq_route_tenant_systems",
               columnNames = {"tenant_id", "source_system_id", "destination_system_id"}))
public class TenantSystemRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Integer tenantId;

    @Column(name = "source_system_id", nullable = false)
    private Integer sourceSystemId;

    @Column(name = "destination_system_id", nullable = false)
    private Integer destinationSystemId;

    @Column(name = "auth_method", nullable = false, length = 30)
    private String authMethod;

    @Column(name = "secrets_path")
    private String secretsPath;

    @Column(name = "cbs_adapter", length = 50)
    private String cbsAdapter;

    @Column(name = "endpoints", length = 500)
    private String endpoints;

    @Column(name = "s3_bucket_url")
    private String s3BucketUrl;

    @Column(name = "s3_bucket_name", length = 100)
    private String s3BucketName;

    @Column(name = "dw_endpoint")
    private String dwEndpoint;

    @Column(name = "dw_database_name", length = 100)
    private String dwDatabaseName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    protected TenantSystemRoute() {}

    public TenantSystemRoute(Integer tenantId, Integer sourceSystemId, Integer destinationSystemId,
                              String authMethod, String secretsPath, String cbsAdapter, String endpoints,
                              String s3BucketUrl, String s3BucketName, String dwEndpoint, String dwDatabaseName,
                              boolean active, Instant updatedAt, String updatedBy) {
        this.tenantId = tenantId;
        this.sourceSystemId = sourceSystemId;
        this.destinationSystemId = destinationSystemId;
        this.authMethod = authMethod;
        this.secretsPath = secretsPath;
        this.cbsAdapter = cbsAdapter;
        this.endpoints = endpoints;
        this.s3BucketUrl = s3BucketUrl;
        this.s3BucketName = s3BucketName;
        this.dwEndpoint = dwEndpoint;
        this.dwDatabaseName = dwDatabaseName;
        this.active = active;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getId() { return id; }
    public Integer getTenantId() { return tenantId; }
    public Integer getSourceSystemId() { return sourceSystemId; }
    public Integer getDestinationSystemId() { return destinationSystemId; }
    public String getAuthMethod() { return authMethod; }
    public String getSecretsPath() { return secretsPath; }
    public String getCbsAdapter() { return cbsAdapter; }
    public String getEndpoints() { return endpoints; }
    public String getS3BucketUrl() { return s3BucketUrl; }
    public String getS3BucketName() { return s3BucketName; }
    public String getDwEndpoint() { return dwEndpoint; }
    public String getDwDatabaseName() { return dwDatabaseName; }
    public boolean isActive() { return active; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}

package com.visionfund.trrs.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "mfi_master")
public class MfiMaster {

    @Id
    @Column(name = "tenant_id")
    private Integer tenantId;

    @Column(name = "mfi_name", nullable = false)
    private String mfiName;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected MfiMaster() {}

    public MfiMaster(Integer tenantId, String mfiName, String countryCode, boolean active) {
        this.tenantId = tenantId;
        this.mfiName = mfiName;
        this.countryCode = countryCode;
        this.active = active;
    }

    public Integer getTenantId() { return tenantId; }
    public String getMfiName() { return mfiName; }
    public String getCountryCode() { return countryCode; }
    public boolean isActive() { return active; }
}

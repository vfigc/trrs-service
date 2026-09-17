package com.visionfund.trrs.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "systems")
public class SystemEntity {

    @Id
    @Column(name = "system_id")
    private Integer systemId;

    @Column(name = "system_code", nullable = false, unique = true, length = 50)
    private String systemCode;

    @Column(name = "system_name", nullable = false)
    private String systemName;

    @Column(name = "system_type", nullable = false, length = 20)
    private String systemType;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected SystemEntity() {}

    public SystemEntity(Integer systemId, String systemCode, String systemName,
                         String systemType, String countryCode, boolean active) {
        this.systemId = systemId;
        this.systemCode = systemCode;
        this.systemName = systemName;
        this.systemType = systemType;
        this.countryCode = countryCode;
        this.active = active;
    }

    public Integer getSystemId() { return systemId; }
    public String getSystemCode() { return systemCode; }
    public String getSystemName() { return systemName; }
    public String getSystemType() { return systemType; }
    public String getCountryCode() { return countryCode; }
    public boolean isActive() { return active; }
}

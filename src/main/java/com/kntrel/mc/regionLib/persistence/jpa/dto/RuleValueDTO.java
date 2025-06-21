package com.kntrel.mc.regionLib.persistence.jpa.dto;

import com.kntrel.mc.regionLib.persistence.jpa.mapper.JpaEntity;
import com.kntrel.mc.regionLib.util.valueType.ValueHolder;
import jakarta.persistence.*;

@Entity
@Table(name = "regionRule")
public class RuleValueDTO implements JpaEntity<ValueHolder<?>> {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private RegionDTO region;

    @Column(name = "key")
    private String key;

    @Column(name = "value")
    private String value;


    //GETTERS
    public Long getId() {
        return this.id;
    }
    public RegionDTO getRegion() {
        return this.region;
    }
    public String getKey() {
        return this.key;
    }
    public String getValue() {
        return this.value;
    }


    //SETTERS
    public void setId(Long id) {
        this.id = id;
    }
    public void setRegion(RegionDTO region) {
        this.region = region;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public void setValue(String value) {
        this.value = value;
    }
}

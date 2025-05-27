package com.jkantrell.regionslib.persistence.jpa.dto;

import com.jkantrell.regionslib.persistence.jpa.mapper.JpaEntity;
import com.jkantrell.regionslib.region.dataContainer.RegionData;
import jakarta.persistence.*;

@Entity
@Table(name = "regionData")
public class RegionDataDTO implements JpaEntity<RegionData> {

    //FIELDS
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
package com.jkantrell.regionslib.persistence.jpa.dto;

import com.jkantrell.regionslib.persistence.jpa.mapper.JpaEntity;
import com.jkantrell.regionslib.region.Region;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "region")
public class RegionDTO implements JpaEntity<Region> {

    //FIELDS
    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "world")
    private String world;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "destroyed")
    private boolean isDestroyed;

    @Column(name = "min_x")
    private Double minX;

    @Column(name = "min_y")
    private Double minY;

    @Column(name = "min_z")
    private Double minZ;

    @Column(name = "max_x")
    private Double maxX;

    @Column(name = "max_y")
    private Double maxY;

    @Column(name = "max_z")
    private Double maxZ;

    @Column(name = "hierarchy_id")
    private Long hierarchyId;

    @OneToMany(mappedBy = "region")
    private List<RuleDTO> rules;

    @OneToMany(mappedBy = "region")
    private List<PermissionDTO> permissions;

    @OneToMany(mappedBy = "region")
    private List<RegionDataDTO> dataContainer;


    //GETTERS
    public Long getId() {
        return this.id;
    }
    public String getName() {
        return this.name;
    }
    public String getWorld() {
        return this.world;
    }
    public List<PermissionDTO> getPermissions() {
        return this.permissions;
    }
    public boolean isEnabled() {
        return this.enabled;
    }
    public boolean isDestroyed() {
        return this.isDestroyed;
    }
    public List<RegionDataDTO> getDataContainer() {
        return this.dataContainer;
    }
    public Double getMinX() {
        return this.minX;
    }
    public Double getMinY() {
        return this.minY;
    }
    public Double getMinZ() {
        return this.minZ;
    }
    public Double getMaxX() {
        return this.maxX;
    }
    public Double getMaxY() {
        return this.maxY;
    }
    public Double getMaxZ() {
        return this.maxZ;
    }
    public Long getHierarchyId() {
        return this.hierarchyId;
    }
    public List<RuleDTO> getRules() {
        return this.rules;
    }

    
    //SETTERS
    public void setId(Long id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setWorld(String world) {
        this.world = world;
    }
    public void setPermissions(List<PermissionDTO> permissions) {
        this.permissions = permissions;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public void setDestroyed(boolean destroyed) {
        this.isDestroyed = destroyed;
    }
    public void setDataContainer(List<RegionDataDTO> dataContainer) {
        this.dataContainer = dataContainer;
    }
    public void setMinX(Double minX) {
        this.minX = minX;
    }
    public void setMinY(Double minY) {
        this.minY = minY;
    }
    public void setMinZ(Double minZ) {
        this.minZ = minZ;
    }
    public void setMaxX(Double maxX) {
        this.maxX = maxX;
    }
    public void setMaxY(Double maxY) {
        this.maxY = maxY;
    }
    public void setMaxZ(Double maxZ) {
        this.maxZ = maxZ;
    }
    public void setHierarchyId(Long hierarchy) {
        this.hierarchyId = hierarchy;
    }
    public void setRules(List<RuleDTO> rules) {
        this.rules = rules;
    }
}
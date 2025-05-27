package com.jkantrell.regionslib.persistence.jpa.dto;

import com.jkantrell.regionslib.persistence.jpa.mapper.JpaEntity;
import com.jkantrell.regionslib.region.Permission;
import jakarta.persistence.*;

@Entity
@Table(name = "regionPermission")
public class PermissionDTO implements JpaEntity<Permission> {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private RegionDTO region;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "level")
    int level;


    //GETTERS
    public Long getId() {
        return this.id;
    }
    public RegionDTO getRegion() {
        return this.region;
    }
    public String getPlayerName() {
        return this.playerName;
    }
    public int getLevel() {
        return this.level;
    }


    //SETTERS
    public void setId(Long id) {
        this.id = id;
    }
    public void setRegion(RegionDTO region) {
        this.region = region;
    }
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    public void setLevel(int level) {
        this.level = level;
    }
}

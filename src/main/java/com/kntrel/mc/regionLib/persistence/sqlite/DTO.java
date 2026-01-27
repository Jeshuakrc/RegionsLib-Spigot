package com.kntrel.mc.regionLib.persistence.sqlite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

final class DTO {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Table { String value(); }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface Column { String value(); }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface Id {}


    @Table("region") record Region(
            @Id @Column("id") long id,
            @Column("name") String name,
            @Column("world") String world,
            @Column("enabled") boolean enabled,
            @Column("hierarchy") long hierarchy,
            @Column("min_x") double minX,
            @Column("min_y") double minY,
            @Column("min_z") double minZ,
            @Column("max_x") double maxX,
            @Column("max_y") double maxY,
            @Column("max_z") double maxZ,
            @Column("destroyed") boolean destroyed
    ) {}

    @Table("regionPermission") record Permission(
            @Id @Column("region_id") long regionId,
            @Id @Column("player_uuid") String playerUUID,
            @Column("level") int level
    ) {}

    @Table("regionRule") record Rule(
            @Id @Column("region_id") long regionId,
            @Id @Column("key") String key,
            @Column("value") String value
    ) {}

    @Table("regionData") record Data(
            @Id @Column("region_id") long regionId,
            @Id @Column("key") String key,
            @Column("value") String value
    ) {}
}

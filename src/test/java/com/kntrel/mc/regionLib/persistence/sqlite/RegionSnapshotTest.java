package com.kntrel.mc.regionLib.persistence.sqlite;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class RegionSnapshotTest {

    @Test
    void testFingerPrints() {
        int len = 2000;

        List<RegionSnapshot> snapshots = snapshots(len);
        for (RegionSnapshot snapshot : snapshots) {
            long regionFP = snapshot.regionFingerprint();
            long permsFP = snapshot.permissionsFingerprint();
            long rulesFP = snapshot.rulesFingerprint();
            long dataFP = snapshot.dataFingerprint();
            long mixedFP = snapshot.fingerPrint();

            assertNotEquals(0, regionFP);
            assertNotEquals(0, permsFP);
            assertNotEquals(0, rulesFP);
            assertNotEquals(0, dataFP);
            assertNotEquals(0, mixedFP);
        }

        for (int i = 0; i < len/2; i++) {
            RegionSnapshot a = snapshots.get(i);
            RegionSnapshot b = snapshots.get(len - i - 1);

            assertNotEquals(a.fingerPrint(), b.fingerPrint());
            assertNotEquals(a.regionFingerprint(), b.regionFingerprint());
            assertNotEquals(a.permissionsFingerprint(), b.permissionsFingerprint());
            assertEquals(a.rulesFingerprint(), b.rulesFingerprint());   // they have same rules
            assertEquals(a.dataFingerprint(), b.dataFingerprint());     // they have same data
        }

        List<RegionSnapshot> copies = snapshots.stream().map(RegionSnapshot::clone).toList();

        for (int i = 0; i < snapshots.size(); i++) {
            RegionSnapshot original = snapshots.get(i);
            RegionSnapshot copy = copies.get(i);

            assertNotSame(original, copy);

            assert original.fingerPrint() == copy.fingerPrint();
            assert original.regionFingerprint() == copy.regionFingerprint();
            assert original.permissionsFingerprint() == copy.permissionsFingerprint();
            assert original.rulesFingerprint() == copy.rulesFingerprint();
            assert original.dataFingerprint() == copy.dataFingerprint();
        }

        copies = copies.stream().map(src -> new RegionSnapshot(
                new DTO.Region(
                        src.region().id() + 10000,
                        src.region().name() + "_modified",
                        src.region().world(),
                        src.region().enabled(),
                        src.region().hierarchy(),
                        src.region().minX(),
                        src.region().minY(),
                        src.region().minZ(),
                        src.region().maxX(),
                        src.region().maxY(),
                        src.region().maxZ(),
                        src.region().destroyed()
                ),
                Arrays.copyOf(src.permissions(), src.permissions().length),
                Arrays.copyOf(src.rules(), src.rules().length),
                Arrays.stream(src.data()).map(d -> new DTO.Data(
                        d.regionId(),
                        d.key() + "_modified",
                        d.value() + "_modified"
                )).toArray(DTO.Data[]::new)
        )).toList();

        for (int i = 0; i < len; i++) {
            RegionSnapshot original = snapshots.get(i);
            RegionSnapshot modified = copies.get(i);

            assertNotEquals(original.fingerPrint(), modified.fingerPrint());
            assertNotEquals(original.regionFingerprint(), modified.regionFingerprint());
            assertEquals(original.permissionsFingerprint(), modified.permissionsFingerprint());
            assertEquals(original.rulesFingerprint(), modified.rulesFingerprint());
            assertNotEquals(original.dataFingerprint(), modified.dataFingerprint());
        }
    }


    //HELPERS
    static List<RegionSnapshot> snapshots(int count) {
        List<RegionSnapshot> regionSnapshots = new ArrayList<>();
        for (int i = 0; i < count; i++) {

            double x1 = ThreadLocalRandom.current().nextDouble(-400, 400),
                    y1 = ThreadLocalRandom.current().nextDouble(-64, 320),
                    z1 = ThreadLocalRandom.current().nextDouble(-400, 400),
                    x2 = ThreadLocalRandom.current().nextDouble(-400, 400),
                    y2 = ThreadLocalRandom.current().nextDouble(-64, 320),
                    z2 =  ThreadLocalRandom.current().nextDouble(-400, 400);

            DTO.Region region = new DTO.Region(
                    i,
                    "Region" + i,
                    "world",
                    true,
                    0,
                    Math.min(x1, x2),
                    Math.min(y1, y2),
                    Math.min(z1, z2),
                    Math.max(x1, x2),
                    Math.max(y1, y2),
                    Math.max(z1, z2),
                    false
            );
            List<DTO.Permission> permissions = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                permissions.add(new DTO.Permission(
                        i,
                        UUID.randomUUID().toString(),
                        ThreadLocalRandom.current().nextInt(0, 10)
                ));
            }
            List<DTO.Rule> rules = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                rules.add(new DTO.Rule(
                        i,
                        "rule_key_" + j,
                        "rule_value_" + j
                ));
            }
            List<DTO.Data> data = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                data.add(new DTO.Data(
                        i,
                        "data_key_" + j,
                        "data_value_" + j
                ));
            }
            regionSnapshots.add(new RegionSnapshot(region, permissions, rules, data));
        }
        return regionSnapshots;
    }
}

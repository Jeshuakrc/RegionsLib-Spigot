package com.kntrel.mc.regionLib.cache;

import com.kntrel.mc.regionLib.test.mock.MockWorld;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class RegionSnapshotTest {

    //CONSTANTS
    private static UUID DUMMY_UUID = MockWorld.mockWorld().getUID();


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
                src.id() + 10000,
                src.name() + "_modified",
                src.world(),
                src.enabled(),
                src.hierarchy(),
                src.minX(),
                src.minY(),
                src.minZ(),
                src.maxX(),
                src.maxY(),
                src.maxZ(),
                src.destroyed(),
                src.permissions(),
                src.rules(),
                src.data().stream().map(d -> new RegionSnapshot.Entry(
                            d.key() + "_modified",
                            d.value() + "_modified"
                    )).toList()
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
    public static List<RegionSnapshot> snapshots(int count) {
        List<RegionSnapshot> regionSnapshots = new ArrayList<>();
        for (int i = 0; i < count; i++) {

            List<RegionSnapshot.Permission> permissions = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                permissions.add(new RegionSnapshot.Permission(
                        UUID.randomUUID(),
                        ThreadLocalRandom.current().nextInt(0, 10)
                ));
            }
            List<RegionSnapshot.Entry> rules = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                rules.add(new RegionSnapshot.Entry(
                        "rule_key_" + j,
                        "rule_value_" + j
                ));
            }
            List<RegionSnapshot.Entry> data = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                data.add(new RegionSnapshot.Entry(
                        "data_key_" + j,
                        "data_value_" + j
                ));
            }

            double x1 = ThreadLocalRandom.current().nextDouble(-400, 400),
                    y1 = ThreadLocalRandom.current().nextDouble(-64, 320),
                    z1 = ThreadLocalRandom.current().nextDouble(-400, 400),
                    x2 = ThreadLocalRandom.current().nextDouble(-400, 400),
                    y2 = ThreadLocalRandom.current().nextDouble(-64, 320),
                    z2 =  ThreadLocalRandom.current().nextDouble(-400, 400);

            RegionSnapshot snap = new RegionSnapshot(
                    i,
                    "Region" + i,
                    DUMMY_UUID,
                    true,
                    0,
                    Math.min(x1, x2),
                    Math.min(y1, y2),
                    Math.min(z1, z2),
                    Math.max(x1, x2),
                    Math.max(y1, y2),
                    Math.max(z1, z2),
                    false,
                    permissions,
                    rules,
                    data
            );
            regionSnapshots.add(snap);
        }
        return regionSnapshots;
    }
}

package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.jetbrains.annotations.NotNull;
import java.util.Set;

final class OrphanRule extends Rule<String> {

    public OrphanRule(@NotNull String name) {
        super(name, Set.of(), ValueType.STRING);
    }
}

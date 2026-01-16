package com.kntrel.mc.regionLib.command.assembler;

import com.kntrel.mc.commvoker.argument.context.ExecutionContext;
import com.kntrel.mc.commvoker.assembler.AssemblyException;
import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import java.util.Optional;
import java.util.function.Supplier;

class RegionLibAssemblerUtils {

    static Optional<RegionContext> findRegionContext(ExecutionContext<?> ctx) {
        RegionContext regionContext = ctx.previousArgumentOfType(RegionContext.class);
        if (regionContext == null) {
            Region region = ctx.previousArgumentOfType(Region.class);
            if (region != null) { regionContext = region.getContext(); }
        }
        if (regionContext == null) {
            regionContext = RegionLib.getDefaultContext();
        }
        return Optional.ofNullable(regionContext);
    }

    static RegionContext findRegionContextOrThrow(ExecutionContext<?> ctx, Supplier<AssemblyException> exceptionSupplier) throws AssemblyException {
        return findRegionContext(ctx).orElseThrow(exceptionSupplier);
    }

    static RegionContext findRegionContextOrThrow(ExecutionContext<?> ctx, String msg) throws AssemblyException {
        return findRegionContextOrThrow(ctx, () -> new AssemblyException(msg));
    }
    static RegionContext findRegionContextOrThrow(ExecutionContext<?> ctx) throws AssemblyException {
        return findRegionContextOrThrow(ctx, () -> new AssemblyException("Unable to resolve region context"));
    }

}

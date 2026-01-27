package com.kntrel.mc.regionLib.command.assembler;

import com.kntrel.mc.commvoker.argument.context.ExecutionContext;
import com.kntrel.mc.commvoker.assembler.Assembler;
import com.kntrel.mc.commvoker.assembler.AssemblyException;
import com.kntrel.mc.commvoker.assembler.TransformAssembler;
import com.kntrel.mc.commvoker.provided.assemblers.StringAssembler;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.kntrel.mc.regionLib.command.assembler.RegionLibAssemblerUtils.*;

public class HierarchyAssembler implements TransformAssembler<Object, String, Hierarchy> {

    //FACTORY
    public static HierarchyAssembler hierarchy() {
        return new HierarchyAssembler();
    }


    //CONSTRUCTOR
    private HierarchyAssembler() {}


    //IMPLEMENTATION
    @Override
    public Assembler<? super Object, ? extends String> delegate() {
        return StringAssembler.string();
    }
    @Override
    public Hierarchy compose(ExecutionContext<?> ctx, String key) throws AssemblyException {

        //Finding the region context
        RegionContext rc = findRegionContextOrThrow(ctx);
        HierarchyRepository hr = rc.getHierarchyRepository();

        //Try by ID first
        try {
            Long id = Long.parseLong(key);
            Hierarchy out = hr.get(id).orElse(null);
            if (out != null) { return out; }
        } catch (NumberFormatException ignored) {}

        //Try by name
        List<Hierarchy> result = hr.getByName(key);
        if (result.size() > 1) {
            throw new AssemblyException("Ambiguous query: there's " + result.size() + " hierarchies called '" + key + "'");
        }
        if (result.isEmpty()) {
            throw new AssemblyException("No hierarchy called '" + key + "' was found");

        }

        return result.getFirst();
    }
    @Override
    public CompletableFuture<Suggestions> suggest(ExecutionContext<?> context, SuggestionsBuilder builder) {
        HierarchyRepository hr = findRegionContext(context)
                .map(RegionContext::getHierarchyRepository)
                .orElse(null);
        if (hr == null) {
            return builder.buildFuture();
        }

        hr.getAll().stream()
                .map(Hierarchy::getName)
                .distinct()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}

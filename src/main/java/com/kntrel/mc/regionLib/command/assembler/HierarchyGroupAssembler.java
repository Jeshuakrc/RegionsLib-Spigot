package com.kntrel.mc.regionLib.command.assembler;

import com.kntrel.mc.commvoker.argument.context.ExecutionContext;
import com.kntrel.mc.commvoker.assembler.Assembler;
import com.kntrel.mc.commvoker.assembler.AssemblyException;
import com.kntrel.mc.commvoker.assembler.TransformAssembler;
import com.kntrel.mc.commvoker.provided.assemblers.StringAssembler;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public class HierarchyGroupAssembler implements TransformAssembler<Object, String, Hierarchy.Group> {

    //FACTORY
    public static final HierarchyGroupAssembler INSTANCE = new HierarchyGroupAssembler();
    public static HierarchyGroupAssembler hierarchyGroup() {
        return INSTANCE;
    }


    //CONSTRUCTOR
    private HierarchyGroupAssembler() {}


    //IMPLEMENTATION
    @Override public Assembler<? super Object, ? extends String> delegate() {
        return StringAssembler.string();
    }
    @Override public Hierarchy.Group compose(ExecutionContext<?> ctx, String object) throws AssemblyException {
        Hierarchy hierarchy = ctx.previousArgumentOfType(Hierarchy.class);

        if (hierarchy == null) {
            Region region = ctx.previousArgumentOfType(Region.class);
            if (region != null) {
                hierarchy = region.getHierarchy();
            }
        }

        if (hierarchy == null) {
            throw new AssemblyException("No hierarchy context found for group '" + object + "'");
        }
        Hierarchy.Group group = hierarchy.getGroup(object).orElse(null);
        if (group == null) {
            throw new AssemblyException("No group called '" + object + "' was found in hierarchy '" + hierarchy.getName() + "'");
        }
        return group;
    }
    @Override public CompletableFuture<Suggestions> suggest(ExecutionContext<?> context, SuggestionsBuilder builder) {
        Hierarchy hierarchy = context.previousArgumentOfType(Hierarchy.class);

        if (hierarchy == null) {
            Region region = context.previousArgumentOfType(Region.class);
            if (region != null) {
                hierarchy = region.getHierarchy();
            }
        }

        if (hierarchy != null) {
            hierarchy.getGroups().stream()
                    .map(Hierarchy.Group::getName)
                    .forEach(builder::suggest);
        }

        return builder.buildFuture();
    }
}

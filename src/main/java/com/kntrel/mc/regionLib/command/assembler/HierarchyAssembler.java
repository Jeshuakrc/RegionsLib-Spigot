package com.kntrel.mc.regionLib.command.assembler;

import com.kntrel.mc.commvoker.argument.context.ExecutionContext;
import com.kntrel.mc.commvoker.assembler.Assembler;
import com.kntrel.mc.commvoker.assembler.TransformAssembler;
import com.kntrel.mc.commvoker.provided.assemblers.StringAssembler;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HierarchyAssembler implements TransformAssembler<Object, String, Hierarchy> {

    //FACTORY
    public static HierarchyAssembler hierarchyFromRegistry(HierarchyRepository hierarchyRepository) {
        return new HierarchyAssembler(hierarchyRepository);
    }


    //FIELDS
    private final HierarchyRepository hierarchyRepository_;


    //CONSTRUCTOR
    private HierarchyAssembler(HierarchyRepository hierarchyRepository) {
        this.hierarchyRepository_ = hierarchyRepository;
    }


    //IMPLEMENTATION
    @Override
    public Assembler<? super Object, ? extends String> delegate() {
        return StringAssembler.string();
    }
    @Override
    public Hierarchy compose(ExecutionContext<?> ctx, String key) {

        Long id = null;
        try {
            id = Long.parseLong(key);
        } catch (NumberFormatException ignored) {}

        if (id != null) {
            Hierarchy out = this.hierarchyRepository_.get(id).orElse(null);
            if (out != null) { return out; }
        }

        List<Hierarchy> result = this.hierarchyRepository_.getByName(key);

        if (result.size() > 1) {
            throw new NullPointerException("Ambiguous query: there's " + result.size() + " hierarchies called '" + key + "'");
        }
        if (result.isEmpty()) {
            throw new NullPointerException("No hierarchy called '" + key + "' was found");

        }

        return result.getFirst();
    }
    @Override
    public CompletableFuture<Suggestions> suggest(ExecutionContext<?> context, SuggestionsBuilder builder) {
        this.hierarchyRepository_.getAll().stream()
                .map(Hierarchy::getName)
                .distinct()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}

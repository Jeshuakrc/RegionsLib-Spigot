package com.kntrel.mc.regionLib.command.assembler;

import com.kntrel.mc.commvoker.argument.context.ExecutionContext;
import com.kntrel.mc.commvoker.assembler.Assembler;
import com.kntrel.mc.commvoker.assembler.TransformAssembler;
import com.kntrel.mc.commvoker.provided.assemblers.StringAssembler;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleRegistry;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RuleAssembler implements TransformAssembler<Object, String, Rule<?>> {

    //FACTORY
    private static final Map<RuleRegistry, RuleAssembler> CACHE = new ConcurrentHashMap<>();
    public static RuleAssembler ruleFromRegistry(RuleRegistry registry) {
        return CACHE.computeIfAbsent(registry, RuleAssembler::new);
    }


    //FIELDS
    private final RuleRegistry registry_;


    //CONSTRUCTOR
    private RuleAssembler(RuleRegistry registry) {
        this.registry_ = registry;
    }


    //IMPLEMENTATION
    @Override
    public Assembler<? super Object, ? extends String> delegate() {
        return StringAssembler.word();
    }
    @Override
    public CompletableFuture<Suggestions> suggest(ExecutionContext<?> context, SuggestionsBuilder builder) {
        this.registry_.getAll().stream()
                .map(Rule::getName)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
    @Override
    public Rule<?> compose(ExecutionContext<?> ctx, String key) {
        return this.registry_.get(key).orElse(null);
    }
}

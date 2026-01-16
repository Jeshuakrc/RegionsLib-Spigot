package com.kntrel.mc.regionLib.command.assembler;

import com.kntrel.mc.commvoker.argument.context.ExecutionContext;
import com.kntrel.mc.commvoker.assembler.Assembler;
import com.kntrel.mc.commvoker.assembler.AssemblyException;
import com.kntrel.mc.commvoker.assembler.TransformAssembler;
import com.kntrel.mc.commvoker.provided.assemblers.StringAssembler;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleRegistry;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;

import static com.kntrel.mc.regionLib.command.assembler.RegionLibAssemblerUtils.*;

public class RuleAssembler implements TransformAssembler<Object, String, Rule<?>> {

    //FACTORY
    public static RuleAssembler rule() {
        return new RuleAssembler();
    }


    //CONSTRUCTOR
    private RuleAssembler() {}


    //IMPLEMENTATION
    @Override
    public Assembler<? super Object, ? extends String> delegate() {
        return StringAssembler.word();
    }
    @Override
    public CompletableFuture<Suggestions> suggest(ExecutionContext<?> context, SuggestionsBuilder builder) {
        RuleRegistry rr = findRegionContext(context)
                .map(RegionContext::getRuleRegistry)
                .orElse(null);
        if (rr == null) {
            return builder.buildFuture();
        }
        rr.getAll().stream()
                .map(Rule::getName)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
    @Override
    public Rule<?> compose(ExecutionContext<?> ctx, String key) throws AssemblyException {
        RegionContext rc = findRegionContextOrThrow(ctx);
        return rc.getRuleRegistry().get(key).orElseThrow(
            () -> new AssemblyException("No rule called '" + key + "' was found")
        );
    }
}

package com.kntrel.mc.regionLib.command.assembler;

import com.kntrel.mc.commvoker.argument.context.ExecutionContext;
import com.kntrel.mc.commvoker.assembler.Assembler;
import com.kntrel.mc.commvoker.assembler.BiComposedAssembler;
import com.kntrel.mc.commvoker.provided.assemblers.StringAssembler;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;

public class RuleValueAssembler implements BiComposedAssembler<Object, Rule<?>, String, RuleValue<?>> {

    //FACTORY
    public static RuleValueAssembler ruleValue() {
        return new RuleValueAssembler();
    }


    //CONSTRUCTOR
    private RuleValueAssembler() {}


    @Override
    public Assembler<? super Object, ? extends Rule<?>> firstDelegate() {
        return RuleAssembler.rule();
    }

    @Override
    public Assembler<? super Object, ? extends String> secondDelegate() {
        return StringAssembler.string();
    }

    @Override
    public RuleValue<?> compose(ExecutionContext<?> ctx, Rule<?> rule, String value) {
        return new RuleValue<>(rule, value);
    }

    @Override
    public CompletableFuture<Suggestions> secondSuggest(ExecutionContext<?> ctx, Rule<?> rule, SuggestionsBuilder suggestionsBuilder) {
        ValueType<?> valueType = rule.valueType();
        if (valueType.equals(ValueType.BOOL)) {
            suggestionsBuilder.suggest("true");
            suggestionsBuilder.suggest("false");
        }
        return suggestionsBuilder.buildFuture();
    }
}

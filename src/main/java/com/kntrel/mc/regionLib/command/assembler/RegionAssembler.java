package com.kntrel.mc.regionLib.command.assembler;

import com.kntrel.mc.commvoker.argument.context.ExecutionContext;
import com.kntrel.mc.commvoker.assembler.Assembler;
import com.kntrel.mc.commvoker.assembler.AssemblyException;
import com.kntrel.mc.commvoker.assembler.TransformAssembler;
import com.kntrel.mc.commvoker.provided.assemblers.StringAssembler;
import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.NamespaceRegionKey;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RegionAssembler implements TransformAssembler<Object, String, Region> {

    //FACTORY
    public static RegionAssembler region() {
        return new RegionAssembler();
    }


    //CONSTRUCTOR
    private RegionAssembler() {}


    //IMPLEMENTATION
    @Override
    public Assembler<? super Object, ? extends String> delegate() {
        return StringAssembler.string();
    }
    @Override
    public Region compose(ExecutionContext<?> ctx, String key) throws AssemblyException {
        NamespaceRegionKey nsKey = parseKey(key);

        RegionContext context = RegionLib.getContext(nsKey.namespace());
        if (context == null) {
            throw new AssemblyException("No region context with namespace '" + nsKey.namespace() + "' is registered");
        }

        if (nsKey.isById()) {
            return context.getRegionRepository()
                    .get(nsKey.id())
                    .orElseThrow(
                            () -> new AssemblyException("Region '" + nsKey + "' doesn't exist.")
                    );
        }

        List<Region> regions = context.get(nsKey.name());
        if (regions.isEmpty()) {
            throw new AssemblyException("Region '" + nsKey + "' doesn't exist.");
        }
        if (regions.size() > 1) {
            throw new AssemblyException("There are regions with name '" + nsKey + "'. Please use ID to disambiguate.");
        }
        return regions.getFirst();

    }
    @Override
    public CompletableFuture<Suggestions> suggest(ExecutionContext<?> context, SuggestionsBuilder builder) {

        //Reading input candidate
        String input = context.commandContext().getInput();
        int start = input.length() - 1;
        boolean read = false, seenWhiteSpace = false, quoted = false;
        for (int i = start; i >= 0; i--) {
            char c = input.charAt(i);
            if (c == ' ' && (!read || !seenWhiteSpace)) {
                seenWhiteSpace = true;
                start = i;
                continue;
            }
            if (c == '\"') {
                quoted = true;
                start = i;
                break;
            }
            read = true;
        }
        input = input.substring(start + 1);
        NamespaceRegionKey ns = null;

        if (read && (input.contains(":") || input.contains("#"))) {
            try { ns = parseKey(input); } catch (AssemblyException ignored) { return builder.buildFuture(); }
        }

        //Resolving context
        List<String> suggestions = null;
        RegionContext ctx = (ns == null)
                ? RegionLib.getDefaultContext()
                : RegionLib.getContext(ns.namespace());
        if (ctx == null) { return builder.buildFuture(); }


        //Is namespaced
        if (ns != null) {
            if (ns.isById()) {
                suggestions = suggestById(ctx, ns.id());
            } else {
                suggestions = suggestByName(ctx, ns.name());
            }
        // Using default context
        } else {
            if (read) try {
                long id = Long.parseLong(input);
                suggestions = suggestById(ctx, id);
            } catch (NumberFormatException ignored) {
                suggestions = suggestByName(ctx, input);
            }
        }

        if (suggestions == null || suggestions.isEmpty()) {
            return builder.buildFuture();
        }

        for (String s : suggestions) {
            String suggestion = "";
            if (ns != null) {
                suggestion += ns.namespace() + (ns.isById() ? "#" : ":");
            }
            suggestion += s;
            if (quoted || suggestion.contains(" ")) {
                suggestion = "\"" + suggestion + "\"";
            }
            builder.suggest(suggestion);
        }
        return builder.buildFuture();
    }


    //HELPERS
    private static NamespaceRegionKey parseKey(String raw) throws AssemblyException {
        try {
            return NamespaceRegionKey.of(raw);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new AssemblyException(e.getMessage());
        }
    }
    private static List<String> suggestByName(RegionContext ctx, @Nullable String input) {
        if (input == null || input.isEmpty()) {
            return ctx.where()
                    .orderBy(RegionField.NAME)
                    .limit(10)
                    .get().stream()
                    .map(Region::getName)
                    .distinct()
                    .sorted().toList();
        }

        return ctx.where()
                .nameContains(input)
                .limit(10)
                .get().stream()
                .map(Region::getName)
                .distinct()
                .toList();
    }
    private static List<String> suggestById(RegionContext ctx, long input) {
        List<Condition> conditions = new ArrayList<>(10);
        conditions.add(Condition.equal(RegionField.ID, input));
        long start = input, end = 0;
        for (int i = 0; i < 9; i++) {
            start *= 10;
            end = (end * 10) + 9;
            conditions.add(
                    Condition.between(RegionField.ID, start, input + end)
            );
        }
        return ctx.where(Condition.OR(conditions))
                .orderBy(RegionField.ID)
                .limit(10)
                .get().stream()
                .map(r -> Long.toString(r.getId()))
                .toList();
    }
}

package com.kntrel.mc.regionLib.command.assembler;

import com.kntrel.mc.commvoker.argument.context.ExecutionContext;
import com.kntrel.mc.commvoker.assembler.Assembler;
import com.kntrel.mc.commvoker.assembler.TransformAssembler;
import com.kntrel.mc.commvoker.provided.assemblers.StringAssembler;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.RegionRepository;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class RegionAssembler implements TransformAssembler<Object, String, Region> {

    //FACTORY
    public static RegionAssembler regionFromRepository(RegionContext regionContext) {
        return new RegionAssembler(regionContext);
    }


    //FIELDS
    private final RegionRepository regionRepository_;


    //CONSTRUCTOR
    private RegionAssembler(RegionRepository regionRepository) {
        this.regionRepository_ = regionRepository;
    }


    //IMPLEMENTATION
    @Override
    public Assembler<? super Object, ? extends String> delegate() {
        return StringAssembler.string();
    }
    @Override
    public Region compose(ExecutionContext<?> ctx, String key) {
        List<Region> result = this.regionRepository_.get(key);
        if (result.size() > 1) {
            throw new NullPointerException("Ambiguous query: there's " + result.size() + " regions called '" + key + "'");
        }
        if (!result.isEmpty()) {
            return result.getFirst();
        }

        long id;
        try {
            id = Long.parseLong(key);
        } catch (NumberFormatException e) {
            throw new NullPointerException("No region with name '" + key + "' was found");
        }

        Region single = this.regionRepository_.get(id).filter(r -> !r.isDestroyed()).orElse(null);
        if (single == null) {
            throw new NullPointerException("No region with id " + id + " was found");
        }

        return single;

    }
    @Override
    public CompletableFuture<Suggestions> suggest(ExecutionContext<?> context, SuggestionsBuilder builder) {
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

        if (read) try {
            Integer id = Integer.parseInt(input);
            this.regionRepository_.getAll().stream()
                    .filter(r -> !r.isDestroyed())
                    .map(Region::getId)
                    .map(Object::toString)
                    .forEach(builder::suggest);
            return builder.buildFuture();

        } catch (NumberFormatException ignored) {}

        Stream<String> names = this.regionRepository_.getAll().stream()
                .filter(r -> !r.isDestroyed())
                .map(Region::getName);
        if (read) {
            String finalInput = input;
            names = names.filter(n -> n.toLowerCase().startsWith(finalInput.toLowerCase()));
        }
        names   .map(quoted
                    ? n -> "\"" + n + "\""
                    : n -> n.contains(" ") ? ("\"" + n + "\"") : n
                )
                .distinct()
                .forEach(builder::suggest);

        return builder.buildFuture();
    }
}

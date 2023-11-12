package net.draycia.carbon.common.command.argument;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.parser.ParserDescriptor;
import cloud.commandframework.arguments.suggestion.Suggestion;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.context.CommandInput;
import cloud.commandframework.keys.CloudKey;
import com.google.inject.Inject;
import io.leangen.geantyref.TypeToken;
import net.draycia.carbon.api.users.CarbonPlayer;
import net.draycia.carbon.api.users.UserManager;
import net.draycia.carbon.common.command.Commander;
import net.draycia.carbon.common.command.exception.ComponentException;
import net.draycia.carbon.common.messages.CarbonMessages;
import net.draycia.carbon.common.users.ProfileResolver;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@DefaultQualifier(NonNull.class)
public final class CarbonPlayerParser<C extends Commander> implements ArgumentParser.FutureArgumentParser<C, CarbonPlayer>, ParserDescriptor<C, CarbonPlayer> {

    // This hack only works properly when there is 0 or 1 CarbonPlayerArguments in a chain, since we don't use the arg name
    public static CloudKey<String> INPUT_STRING = CloudKey.of(CarbonPlayerParser.class.getSimpleName() + "-input", TypeToken.get(String.class));

    private final PlayerSuggestions suggestions;
    private final UserManager<?> userManager;
    private final ProfileResolver profileResolver;
    private final CarbonMessages messages;

    @Inject
    private CarbonPlayerParser(
        final PlayerSuggestions suggestions,
        final UserManager<?> userManager,
        final ProfileResolver profileResolver,
        final CarbonMessages messages
    ) {
        this.suggestions = suggestions;
        this.userManager = userManager;
        this.profileResolver = profileResolver;
        this.messages = messages;
    }

    @Override
    public CompletableFuture<CarbonPlayer> parseFuture(
        final CommandContext<C> commandContext,
        final CommandInput commandInput
    ) {
        final String input = commandInput.readString();
        return this.profileResolver.resolveUUID(input, commandContext.isSuggestions()).thenCompose(uuid -> {
            if (uuid == null) {
                throw new ParseException(input, this.messages);
            }
            commandContext.store(INPUT_STRING, input);
            return this.userManager.user(uuid);
        }).thenApply(Function.identity());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Suggestion> suggestions(
        final CommandContext<C> commandContext,
        final String input
    ) {
        return this.suggestions.suggestions((CommandContext<Commander>) commandContext, input);
    }

    @Override
    public @NonNull TypeToken<CarbonPlayer> valueType() {
        return TypeToken.get(CarbonPlayer.class);
    }

    @Override
    public @NonNull ArgumentParser<C, CarbonPlayer> parser() {
        return this;
    }


    public static final class ParseException extends ComponentException {

        private static final long serialVersionUID = -8331761537951077684L;
        private final String input;

        public ParseException(final String input, final CarbonMessages messages) {
            super(messages.errorCommandInvalidPlayer(input));
            this.input = input;
        }

        public String input() {
            return this.input;
        }

    }
}

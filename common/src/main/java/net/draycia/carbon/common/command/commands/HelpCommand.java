/*
 * CarbonChat
 *
 * Copyright (c) 2023 Josua Parks (Vicarious)
 *                    Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.draycia.carbon.common.command.commands;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.suggestion.Suggestion;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.help.result.CommandEntry;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.minecraft.extras.RichDescription;
import com.google.inject.Inject;
import java.util.List;
import net.draycia.carbon.common.command.CarbonCommand;
import net.draycia.carbon.common.command.CommandSettings;
import net.draycia.carbon.common.command.Commander;
import net.draycia.carbon.common.messages.CarbonMessageSource;
import net.draycia.carbon.common.messages.CarbonMessages;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import static cloud.commandframework.arguments.standard.StringParser.greedyStringParser;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextColor.color;

@DefaultQualifier(NonNull.class)
public final class HelpCommand extends CarbonCommand {

    private final CommandManager<Commander> commandManager;
    private final CarbonMessages carbonMessages;
    private final MinecraftHelp<Commander> minecraftHelp;

    @Inject
    public HelpCommand(
        final CommandManager<Commander> commandManager,
        final CarbonMessageSource messageSource,
        final CarbonMessages carbonMessages
    ) {
        this.commandManager = commandManager;
        this.carbonMessages = carbonMessages;
        this.minecraftHelp = createHelp(commandManager, messageSource);
    }

    @Override
    public CommandSettings defaultCommandSettings() {
        return new CommandSettings("carbon");
    }

    @Override
    public Key key() {
        return Key.key("carbon", "help");
    }

    @Override
    public void init() {
        final var command = this.commandManager.commandBuilder(this.commandSettings().name(), this.commandSettings().aliases())
            .literal("help", RichDescription.of(this.carbonMessages.commandHelpDescription()))
            .optional("query", greedyStringParser(), RichDescription.of(this.carbonMessages.commandHelpArgumentQuery()), this::suggestQueries)
            .permission("carbon.help")
            .handler(this::execute)
            .build();

        this.commandManager.command(command);
    }

    private void execute(final CommandContext<Commander> ctx) {
        this.minecraftHelp.queryCommands(ctx.getOrDefault("query", ""), ctx.getSender());
    }

    private List<Suggestion> suggestQueries(final CommandContext<Commander> ctx, final String input) {
        final var result = this.commandManager.createHelpHandler().queryRootIndex(ctx.getSender());
        return result.entries().stream().map(CommandEntry::syntax).map(Suggestion::simple).toList();
    }

    private static MinecraftHelp<Commander> createHelp(
        final CommandManager<Commander> manager,
        final CarbonMessageSource messageSource
    ) {
        final MinecraftHelp<Commander> help = new MinecraftHelp<>(
            "/carbon help",
            AudienceProvider.nativeAudience(),
            manager
        );

        help.setHelpColors(
            MinecraftHelp.HelpColors.of(
                color(0xE099FF),
                WHITE,
                color(0xDD1BC4),
                GRAY,
                DARK_GRAY
            )
        );

        help.messageProvider((sender, key, args) -> {
            final String messageKey = "command.help.misc." + key;
            final TagResolver.Builder tagResolver = TagResolver.builder();

            // Total hack but works for now
            if (args.size() == 2) {
                tagResolver
                    .tag("page", Tag.selfClosingInserting(text(args.get("page"))))
                    .tag("max_pages", Tag.selfClosingInserting(text(args.get("max_pages")))
                );
            }

            return MiniMessage.miniMessage().deserialize(messageSource.messageOf(sender, messageKey), tagResolver.build());
        });

        return help;
    }

}

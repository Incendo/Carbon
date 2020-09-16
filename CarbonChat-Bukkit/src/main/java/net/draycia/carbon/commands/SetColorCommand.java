package net.draycia.carbon.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import net.draycia.carbon.CarbonChatBukkit;
import net.draycia.carbon.api.channels.ChatChannel;
import net.draycia.carbon.api.users.ChatUser;
import net.draycia.carbon.api.commands.CommandSettings;
import net.draycia.carbon.api.users.UserChannelSettings;
import net.draycia.carbon.util.CarbonUtils;
import net.draycia.carbon.util.CommandUtils;
import net.kyori.adventure.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.LinkedHashMap;

public class SetColorCommand {

  @NonNull
  private final CarbonChatBukkit carbonChat;

  public SetColorCommand(@NonNull final CarbonChatBukkit carbonChat, @NonNull final CommandSettings commandSettings) {
    this.carbonChat = carbonChat;

    if (!commandSettings.enabled()) {
      return;
    }

    CommandUtils.handleDuplicateCommands(commandSettings);

    for (final ChatChannel channel : this.carbonChat.channelRegistry()) {
      final LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
      arguments.put("channel", new LiteralArgument(channel.key()));
      arguments.put("color", CarbonUtils.textColorArgument());

      new CommandAPICommand(commandSettings.name())
        .withArguments(arguments)
        .withAliases(commandSettings.aliases())
        .withPermission(CommandPermission.fromString("carbonchat.setcolor"))
        .executesPlayer((player, args) -> {
          final TextColor color = (TextColor) args[0];
          final ChatUser user = this.carbonChat.userService().wrap(player.getUniqueId());

          if (!player.hasPermission("carbonchat.setcolor." + channel.key())) {
            user.sendMessage(carbonChat.messageProcessor().processMessage(
              this.carbonChat.translations().cannotSetColor(),
              "br", "\n", "input", color.asHexString(),
              "channel", channel.name()));

            return;
          }

          final UserChannelSettings settings = user.channelSettings(channel);

          settings.color(color);

          user.sendMessage(carbonChat.messageProcessor().processMessage(
            this.carbonChat.translations().channelColorSet(),
            "br", "\n", "color", "<color:" + color.asHexString() + ">", "channel",
            channel.name(), "hex", color.asHexString()));
        })
        .register();
    }
  }

}

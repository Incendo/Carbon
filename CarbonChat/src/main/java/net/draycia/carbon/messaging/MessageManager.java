package net.draycia.carbon.messaging;

import net.draycia.carbon.CarbonChat;
import net.draycia.carbon.channels.ChatChannel;
import net.draycia.carbon.messaging.impl.BungeeMessageService;
import net.draycia.carbon.messaging.impl.RedisMessageService;
import net.draycia.carbon.storage.ChatUser;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.UUID;

public class MessageManager {

    private final CarbonChat carbonChat;
    private final MessageService messageService;

    private final GsonComponentSerializer gsonSerializer;

    public MessageManager(CarbonChat carbonChat) {
        this.carbonChat = carbonChat;
        this.gsonSerializer = carbonChat.getAdventureManager().getAudiences().gsonSerializer();

        if (carbonChat.getConfig().getBoolean("redis.enabled")) {
            carbonChat.getLogger().info("Using Redis for message forwarding!");
            this.messageService = new RedisMessageService(carbonChat);
        } else {
            carbonChat.getLogger().info("Using Bungee Plugin Messaging for message forwarding!");
            this.messageService = new BungeeMessageService(carbonChat);
        }

        this.registerDefaultListeners();
    }

    private void registerDefaultListeners() {
        getMessageService().registerMessageListener("nickname", (user, byteArray) -> {
            String nickname = byteArray.readUTF();

            user.setNickname(nickname, true);

            if (user.isOnline()) {
                String message = carbonChat.getConfig().getString("language.nickname-set");

                user.sendMessage(carbonChat.getAdventureManager().processMessageWithPapi(user.asPlayer(),
                        message, "nickname", nickname));
            }
        });

        getMessageService().registerMessageListener("nickname-reset", (user, byteArray) -> {
            user.setNickname(null, true);

            if (user.isOnline()) {
                String message = carbonChat.getConfig().getString("language.nickname-reset");

                user.sendMessage(carbonChat.getAdventureManager().processMessageWithPapi(user.asPlayer(), message));
            }
        });

        getMessageService().registerMessageListener("selected-channel", (user, byteArray) -> {
            user.setSelectedChannel(carbonChat.getChannelManager().getRegistry().get(byteArray.readUTF()), true);
        });

        getMessageService().registerMessageListener("spying-whispers", (user, byteArray) -> {
            user.setSpyingWhispers(byteArray.readBoolean(), true);
        });

        getMessageService().registerMessageListener("muted", (user, byteArray) -> {
            user.setMuted(byteArray.readBoolean(), true);
        });

        getMessageService().registerMessageListener("shadow-muted", (user, byteArray) -> {
            user.setShadowMuted(byteArray.readBoolean(), true);
        });

        getMessageService().registerMessageListener("reply-target", (user, byteArray) -> {
            user.setReplyTarget(new UUID(byteArray.readLong(), byteArray.readLong()), true);
        });

        getMessageService().registerMessageListener("ignoring-user", (user, byteArray) -> {
            user.setIgnoringUser(new UUID(byteArray.readLong(), byteArray.readLong()), byteArray.readBoolean(), true);
        });

        getMessageService().registerMessageListener("ignoring-channel", (user, byteArray) -> {
            user.getChannelSettings(carbonChat.getChannelManager().getRegistry().get(byteArray.readUTF()))
                    .setIgnoring(byteArray.readBoolean(), true);
        });

        getMessageService().registerMessageListener("spying-channel", (user, byteArray) -> {
            user.getChannelSettings(carbonChat.getChannelManager().getRegistry().get(byteArray.readUTF()))
                    .setSpying(byteArray.readBoolean(), true);
        });

        getMessageService().registerMessageListener("channel-color", (user, byteArray) -> {
            user.getChannelSettings(carbonChat.getChannelManager().getRegistry().get(byteArray.readUTF()))
                    .setColor(TextColor.fromHexString(byteArray.readUTF()), true);
        });

        getMessageService().registerMessageListener("channel-color-reset", (user, byteArray) -> {
            user.getChannelSettings(carbonChat.getChannelManager().getRegistry().get(byteArray.readUTF()))
                    .setColor(null, true);
        });

        getMessageService().registerMessageListener("channel-component", (user, byteArray) -> {
            ChatChannel channel = carbonChat.getChannelManager().getRegistry().get(byteArray.readUTF());

            if (channel != null) {
                channel.sendComponent(user, gsonSerializer.deserialize(byteArray.readUTF()));
            }
        });

        getMessageService().registerMessageListener("whisper-component", (user, byteArray) -> {
            UUID recipient = new UUID(byteArray.readLong(), byteArray.readLong());

            ChatUser target = carbonChat.getUserService().wrap(recipient);
            String message = byteArray.readUTF();

            if (!target.isIgnoringUser(user.getUUID())) {
                target.setReplyTarget(user.getUUID());
                target.sendMessage(gsonSerializer.deserialize(message));
            }
        });
    }

    public MessageService getMessageService() {
        return messageService;
    }

}

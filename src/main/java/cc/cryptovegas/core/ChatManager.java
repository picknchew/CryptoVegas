package cc.cryptovegas.core;

import com.google.common.collect.ImmutableSet;
import me.clip.deluxechat.DeluxeChat;
import me.clip.deluxechat.events.DeluxeChatEvent;
import me.clip.deluxechat.fanciful.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;
import tech.rayline.core.parse.ResourceFile;
import tech.rayline.core.plugin.YAMLConfigurationFile;

import java.util.HashSet;
import java.util.Set;

@Injectable
public class ChatManager {
    private final DeluxeChat deluxeChat;
    private final Set<String> censored = new HashSet<>();

    @ResourceFile(raw = true, filename = "censor.yml")
    private YAMLConfigurationFile config;

    @InjectionProvider
    public ChatManager(Core core) {
        core.getResourceFileGraph().addObject(this);

        if (config.getConfig().contains("censored")) {
            censored.addAll(config.getConfig().getStringList("censored"));
        }

        this.deluxeChat = (DeluxeChat) Bukkit.getPluginManager().getPlugin("DeluxeChat");

        core.observeEvent(DeluxeChatEvent.class)
                .doOnNext(event -> {
                    String message = event.getChatMessage();
                    String censoredMessage = getCensoredMessage(message);
                    Player player = event.getPlayer();

                    if (!message.equals(censoredMessage)) {
                        event.setChatMessage(censoredMessage);
                        event.getRecipients().remove(player);

                        FancyMessage fancyMessage = deluxeChat.getFancyChatFormat(player, event.getDeluxeFormat());

                        String chatMessage = fancyMessage.getLastColor() + fancyMessage.getChatColor() + message;

                        deluxeChat.getChat().sendDeluxeChat(player, fancyMessage.toJSONString(), deluxeChat.getChat().convertMsg(player, chatMessage), ImmutableSet.of(player));
                    }
                }).subscribe();
    }

    private String getCensoredMessage(String message) {
        String censoredMessage = message;

        for (String word : censored) {
            censoredMessage = censoredMessage.replace(word, getStars(word.length()));
        }

        return censoredMessage;
    }

    private String getStars(int length) {
        StringBuilder builder = new StringBuilder();

        for (int x = 0; x < length; x++) {
            builder.append("*");
        }

        return builder.toString();
    }
}

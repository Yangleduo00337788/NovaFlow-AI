package ai.novaflow.aiengine.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "novaflow:chat:memory:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(key(memoryId));
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(ChatMessageDeserializer.messagesFromJson(json));
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = key(memoryId);
        if (messages == null || messages.isEmpty()) {
            redisTemplate.delete(key);
            return;
        }
        redisTemplate.opsForValue().set(key, ChatMessageSerializer.messagesToJson(messages), TTL);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(key(memoryId));
    }

    private String key(Object memoryId) {
        return KEY_PREFIX + memoryId;
    }
}

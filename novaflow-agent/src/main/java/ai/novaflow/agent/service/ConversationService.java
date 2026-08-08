package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.vo.RetrievalSourceVO;
import ai.novaflow.agent.entity.ConversationEntity;
import ai.novaflow.agent.entity.ConversationMessageEntity;
import ai.novaflow.agent.mapper.ConversationMapper;
import ai.novaflow.agent.mapper.ConversationMessageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void persistExchange(ExchangeRequest request) {
        if (!StringUtils.hasText(request.conversationKey()) || !StringUtils.hasText(request.userMessage())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        ConversationEntity conversation = conversationMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("agent_id", request.agentId())
                        .eq("conversation_key", request.conversationKey())
                        .limit(1)
        );
        if (conversation == null) {
            conversation = new ConversationEntity();
            conversation.setTenantId(request.tenantId());
            conversation.setAgentId(request.agentId());
            conversation.setConversationKey(request.conversationKey());
            conversation.setChannel(request.channel());
            conversation.setUserId(request.userId());
            conversation.setMessageCount(0);
            conversation.setCreatedAt(now);
            conversation.setUpdatedAt(now);
            conversationMapper.insert(conversation);
        } else {
            conversation.setChannel(request.channel());
            conversation.setUserId(request.userId());
            conversation.setUpdatedAt(now);
        }

        insertMessage(conversation, "user", request.userMessage(), null, null, null);
        insertMessage(
                conversation,
                "assistant",
                request.assistantReply(),
                request.tokensUsed(),
                request.latencyMs(),
                request.sources()
        );

        int messageCount = safeInt(conversation.getMessageCount()) + 2;
        conversation.setMessageCount(messageCount);
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.update(conversation);
    }

    private void insertMessage(
            ConversationEntity conversation,
            String role,
            String content,
            Integer tokensUsed,
            Long latencyMs,
            List<RetrievalSourceVO> sources) {
        ConversationMessageEntity message = new ConversationMessageEntity();
        message.setTenantId(conversation.getTenantId());
        message.setConversationId(conversation.getId());
        message.setRole(role);
        message.setContent(content);
        message.setTokensUsed(tokensUsed);
        message.setLatencyMs(latencyMs);
        message.setRetrievalSources(serializeSources(sources));
        message.setCreatedAt(LocalDateTime.now());
        conversationMessageMapper.insert(message);
    }

    private String serializeSources(List<RetrievalSourceVO> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize retrieval sources", e);
            return null;
        }
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    @Builder
    public record ExchangeRequest(
            Long tenantId,
            Long agentId,
            String conversationKey,
            String channel,
            Long userId,
            String userMessage,
            String assistantReply,
            Integer tokensUsed,
            Long latencyMs,
            List<RetrievalSourceVO> sources
    ) {
    }
}

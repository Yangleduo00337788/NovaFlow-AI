package ai.novaflow.chat.service;

import ai.novaflow.chat.domain.ConversationPreviewRow;
import ai.novaflow.chat.domain.vo.ConversationMessageVO;
import ai.novaflow.chat.domain.vo.ConversationVO;
import ai.novaflow.chat.domain.vo.RetrievalSourceVO;
import ai.novaflow.chat.entity.ConversationEntity;
import ai.novaflow.chat.entity.ConversationMessageEntity;
import ai.novaflow.chat.mapper.ConversationMapper;
import ai.novaflow.chat.mapper.ConversationMessageMapper;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ObjectMapper objectMapper;

    public PageResult<ConversationVO> pageConversations(
            Long agentId,
            Long tenantId,
            String channel,
            int page,
            int pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("agent_id", agentId);
        if (StringUtils.hasText(channel)) {
            query.eq("channel", channel);
        }
        query.orderBy("last_message_at", false).orderBy("id", false);

        Page<ConversationEntity> result = conversationMapper.paginate(Page.of(page, pageSize), query);
        Map<Long, String> previewMap = loadPreviewMap(
                tenantId,
                result.getRecords().stream().map(ConversationEntity::getId).toList());
        List<ConversationVO> list = result.getRecords().stream()
                .map(entity -> toConversationVO(entity, previewMap.getOrDefault(entity.getId(), "")))
                .toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    public List<ConversationMessageVO> listMessages(Long agentId, Long tenantId, String conversationKey) {
        ConversationEntity conversation = getConversationOrThrow(agentId, tenantId, conversationKey);
        List<ConversationMessageEntity> messages = conversationMessageMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("conversation_id", conversation.getId())
                        .eq("tenant_id", tenantId)
                        .orderBy("created_at", true)
        );
        return messages.stream().map(this::toMessageVO).toList();
    }

    private ConversationEntity getConversationOrThrow(Long agentId, Long tenantId, String conversationKey) {
        ConversationEntity conversation = conversationMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("agent_id", agentId)
                        .eq("tenant_id", tenantId)
                        .eq("conversation_key", conversationKey)
                        .limit(1)
        );
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }
        return conversation;
    }

    private ConversationVO toConversationVO(ConversationEntity entity, String preview) {
        return ConversationVO.builder()
                .id(entity.getId())
                .conversationKey(entity.getConversationKey())
                .channel(entity.getChannel())
                .messageCount(entity.getMessageCount())
                .preview(preview)
                .lastMessageAt(entity.getLastMessageAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Map<Long, String> loadPreviewMap(Long tenantId, List<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Map.of();
        }
        List<ConversationPreviewRow> rows = conversationMessageMapper.listLatestUserPreviews(tenantId, conversationIds);
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> previewMap = new HashMap<>();
        for (ConversationPreviewRow row : rows) {
            previewMap.put(row.getConversationId(), truncatePreview(row.getContent()));
        }
        return previewMap;
    }

    private String truncatePreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) + "..." : trimmed;
    }

    private ConversationMessageVO toMessageVO(ConversationMessageEntity entity) {
        return ConversationMessageVO.builder()
                .id(entity.getId())
                .role(entity.getRole())
                .content(entity.getContent())
                .tokensUsed(entity.getTokensUsed())
                .latencyMs(entity.getLatencyMs())
                .sources(parseSources(entity.getRetrievalSources()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private List<RetrievalSourceVO> parseSources(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<RetrievalSourceVO>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse retrieval sources", e);
            return List.of();
        }
    }

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
            if (conversation.getId() == null) {
                conversation = conversationMapper.selectOneByQuery(
                        QueryWrapper.create()
                                .eq("agent_id", request.agentId())
                                .eq("conversation_key", request.conversationKey())
                                .limit(1)
                );
                if (conversation == null) {
                    log.warn("Conversation insert succeeded but id missing, agentId={}, key={}",
                            request.agentId(), request.conversationKey());
                    return;
                }
            }
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

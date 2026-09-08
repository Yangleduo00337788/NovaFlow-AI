package ai.novaflow.chat.service;

import ai.novaflow.chat.entity.ConversationEntity;
import ai.novaflow.chat.mapper.ConversationMapper;
import ai.novaflow.chat.mapper.ConversationMessageMapper;
import ai.novaflow.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationMapper conversationMapper;
    @Mock
    private ConversationMessageMapper conversationMessageMapper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void persistExchangeSkipsBlankConversationKeyOrUserMessage() {
        conversationService.persistExchange(ConversationService.ExchangeRequest.builder()
                .conversationKey(" ")
                .userMessage("hello")
                .build());
        conversationService.persistExchange(ConversationService.ExchangeRequest.builder()
                .conversationKey("conv-1")
                .userMessage("")
                .build());
        verify(conversationMapper, never()).insert(any());
        verify(conversationMessageMapper, never()).insert(any());
    }

    @Test
    void persistExchangeCreatesConversationAndTwoMessages() {
        when(conversationMapper.selectOneByQuery(any())).thenReturn(null);
        doAnswer(invocation -> {
            ConversationEntity entity = invocation.getArgument(0);
            entity.setId(42L);
            return 1;
        }).when(conversationMapper).insert(any(ConversationEntity.class));

        conversationService.persistExchange(ConversationService.ExchangeRequest.builder()
                .tenantId(1L)
                .agentId(2L)
                .conversationKey("conv-new")
                .channel("open")
                .callerId("caller-12345678")
                .userMessage("hello")
                .assistantReply("hi there")
                .tokensUsed(12)
                .latencyMs(50L)
                .build());

        verify(conversationMapper).insert(any(ConversationEntity.class));
        verify(conversationMessageMapper, org.mockito.Mockito.times(2)).insert(any());

        ArgumentCaptor<ConversationEntity> updated = ArgumentCaptor.forClass(ConversationEntity.class);
        verify(conversationMapper).update(updated.capture());
        assertEquals(2, updated.getValue().getMessageCount());
    }

    @Test
    void listMessagesThrowsWhenConversationMissing() {
        when(conversationMapper.selectOneByQuery(any())).thenReturn(null);
        assertThrows(BusinessException.class, () ->
                conversationService.listMessages(1L, 1L, "missing-key", null, null));
    }
}

package ai.novaflow.chat.service;

import ai.novaflow.chat.entity.ConversationEntity;
import ai.novaflow.chat.mapper.ConversationMapper;
import ai.novaflow.chat.mapper.ConversationMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationRetentionServiceTest {

    @Mock
    private ConversationMapper conversationMapper;
    @Mock
    private ConversationMessageMapper conversationMessageMapper;

    @InjectMocks
    private ConversationRetentionService conversationRetentionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(conversationRetentionService, "retentionDays", 90);
        ReflectionTestUtils.setField(conversationRetentionService, "retentionEnabled", true);
    }

    @Test
    void purgeDoesNothingWhenRetentionDisabled() {
        ReflectionTestUtils.setField(conversationRetentionService, "retentionEnabled", false);
        conversationRetentionService.purgeExpiredConversations();
        verify(conversationMapper, never()).selectListByQuery(any());
    }

    @Test
    void purgeDoesNothingWhenRetentionDaysNotPositive() {
        ReflectionTestUtils.setField(conversationRetentionService, "retentionDays", 0);
        conversationRetentionService.purgeExpiredConversations();
        verify(conversationMapper, never()).selectListByQuery(any());
    }

    @Test
    void purgeDeletesMessagesAndConversationsWhenExpiredFound() {
        ConversationEntity expired = new ConversationEntity();
        expired.setId(7L);
        when(conversationMapper.selectListByQuery(any())).thenReturn(List.of(expired));

        conversationRetentionService.purgeExpiredConversations();

        verify(conversationMessageMapper).deleteByQuery(any());
        verify(conversationMapper).deleteByQuery(any());
    }

    @Test
    void purgeSkipsDeleteWhenNoExpiredConversations() {
        when(conversationMapper.selectListByQuery(any())).thenReturn(List.of());
        conversationRetentionService.purgeExpiredConversations();
        verify(conversationMessageMapper, never()).deleteByQuery(any());
        verify(conversationMapper, never()).deleteByQuery(any());
    }
}

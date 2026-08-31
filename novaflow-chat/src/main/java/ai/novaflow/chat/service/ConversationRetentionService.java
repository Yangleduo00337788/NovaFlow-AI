package ai.novaflow.chat.service;

import ai.novaflow.chat.entity.ConversationEntity;
import ai.novaflow.chat.mapper.ConversationMapper;
import ai.novaflow.chat.mapper.ConversationMessageMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationRetentionService {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;

    @Value("${novaflow.conversation.retention-days:90}")
    private int retentionDays;

    @Value("${novaflow.conversation.retention-enabled:true}")
    private boolean retentionEnabled;

    @Scheduled(cron = "${novaflow.conversation.retention-cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpiredConversations() {
        if (!retentionEnabled || retentionDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Long> conversationIds = conversationMapper.selectListByQuery(
                QueryWrapper.create()
                        .select("id")
                        .lt("last_message_at", cutoff)
                        .limit(500)
        ).stream().map(ConversationEntity::getId).toList();
        if (conversationIds.isEmpty()) {
            return;
        }
        conversationMessageMapper.deleteByQuery(
                QueryWrapper.create().in("conversation_id", conversationIds));
        int deleted = conversationMapper.deleteByQuery(
                QueryWrapper.create().in("id", conversationIds));
        if (deleted > 0) {
            log.info("Purged {} expired conversations older than {} days", deleted, retentionDays);
        }
    }
}

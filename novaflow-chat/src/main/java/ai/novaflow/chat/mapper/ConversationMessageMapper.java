package ai.novaflow.chat.mapper;

import ai.novaflow.chat.domain.ConversationPreviewRow;
import ai.novaflow.chat.entity.ConversationMessageEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageEntity> {

    List<ConversationPreviewRow> listLatestUserPreviews(
            @Param("tenantId") Long tenantId,
            @Param("conversationIds") List<Long> conversationIds);
}

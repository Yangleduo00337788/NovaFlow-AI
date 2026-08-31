package ai.novaflow.chat.mapper;

import ai.novaflow.chat.entity.ConversationMessageEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageEntity> {
}

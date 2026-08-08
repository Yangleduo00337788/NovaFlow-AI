package ai.novaflow.agent.mapper;

import ai.novaflow.agent.entity.ConversationMessageEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageEntity> {
}

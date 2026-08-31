package ai.novaflow.chat.mapper;

import ai.novaflow.chat.entity.ConversationEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<ConversationEntity> {
}

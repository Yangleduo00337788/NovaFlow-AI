package ai.novaflow.knowledge.mapper;

import ai.novaflow.knowledge.entity.KnowledgeBaseEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {
}

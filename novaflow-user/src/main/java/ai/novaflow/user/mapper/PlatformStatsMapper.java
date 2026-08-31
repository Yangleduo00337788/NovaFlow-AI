package ai.novaflow.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlatformStatsMapper {

    @Select("SELECT COUNT(*) FROM agent WHERE is_deleted = 0")
    Long countAgents();

    @Select("SELECT COUNT(*) FROM knowledge_base WHERE is_deleted = 0")
    Long countKnowledgeBases();

    @Select("SELECT COUNT(*) FROM workflow WHERE is_deleted = 0")
    Long countWorkflows();
}

package ai.novaflow.user.mapper;

import ai.novaflow.model.domain.ProviderCodeAggregate;
import ai.novaflow.model.domain.UsageTrendPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PlatformStatsMapper {

    @Select("SELECT COUNT(*) FROM agent WHERE is_deleted = 0")
    Long countAgents();

    @Select("SELECT COUNT(*) FROM knowledge_base WHERE is_deleted = 0")
    Long countKnowledgeBases();

    @Select("SELECT COUNT(*) FROM workflow WHERE is_deleted = 0")
    Long countWorkflows();

    @Select("SELECT COUNT(*) FROM model_provider WHERE is_deleted = 0")
    Long countModelProviders();

    @Select("SELECT COUNT(*) FROM model_provider WHERE is_deleted = 0 AND is_enabled = 1")
    Long countEnabledModelProviders();

    @Select("SELECT COUNT(*) FROM model_config WHERE is_deleted = 0")
    Long countModelConfigs();

    @Select("SELECT COUNT(*) FROM model_config WHERE is_deleted = 0 AND is_enabled = 1")
    Long countEnabledModelConfigs();

    @Select("""
            SELECT provider_code AS providerCode, COUNT(*) AS count
            FROM model_provider
            WHERE is_deleted = 0
            GROUP BY provider_code
            ORDER BY count DESC
            """)
    List<ProviderCodeAggregate> groupProvidersByCode();

    @Select("SELECT COUNT(*) FROM agent WHERE tenant_id = #{tenantId} AND is_deleted = 0")
    Long countAgentsByTenant(Long tenantId);

    @Select("SELECT COUNT(*) FROM knowledge_base WHERE tenant_id = #{tenantId} AND is_deleted = 0")
    Long countKnowledgeBasesByTenant(Long tenantId);

    @Select("SELECT COUNT(*) FROM application WHERE tenant_id = #{tenantId} AND is_deleted = 0")
    Long countApplicationsByTenant(Long tenantId);

    @Select("SELECT COUNT(*) FROM workflow WHERE tenant_id = #{tenantId} AND is_deleted = 0")
    Long countWorkflowsByTenant(Long tenantId);

    @Select("SELECT COALESCE(SUM(file_size), 0) FROM document WHERE tenant_id = #{tenantId} AND is_deleted = 0")
    Long sumStorageBytesByTenant(Long tenantId);

    @Select("""
            SELECT DATE_FORMAT(day_col, '%m-%d') AS label, COUNT(*) AS value
            FROM (
                SELECT DATE(created_at) AS day_col
                FROM tenant
                WHERE is_deleted = 0 AND created_at >= #{since}
            ) t
            GROUP BY day_col
            ORDER BY day_col
            """)
    List<UsageTrendPoint> tenantDailyGrowth(@Param("since") LocalDateTime since);
}

package ai.novaflow.model.mapper;

import ai.novaflow.model.domain.ModelUsageAggregate;
import ai.novaflow.model.domain.TokenUsageLogRow;
import ai.novaflow.model.domain.UsageTrendPoint;
import ai.novaflow.model.domain.UsageTypeAggregate;
import ai.novaflow.model.entity.TokenUsageEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsageEntity> {

    @Select("""
            SELECT COUNT(*) FROM token_usage WHERE tenant_id = #{tenantId}
            """)
    Long countCallsByTenant(Long tenantId);

    @Select("""
            SELECT COALESCE(SUM(total_tokens), 0) FROM token_usage WHERE tenant_id = #{tenantId}
            """)
    Long sumTokensByTenant(Long tenantId);

    @Select("""
            SELECT COALESCE(SUM(cost), 0) FROM token_usage
            WHERE tenant_id = #{tenantId} AND currency = #{currency}
            """)
    BigDecimal sumCostByTenantAndCurrency(Long tenantId, String currency);

    @Select("""
            SELECT mc.model_name AS modelName,
                   mc.display_name AS displayName,
                   COUNT(*) AS calls,
                   COALESCE(SUM(tu.total_tokens), 0) AS tokens
            FROM token_usage tu
            LEFT JOIN model_config mc ON tu.model_config_id = mc.id
            WHERE tu.tenant_id = #{tenantId}
            GROUP BY tu.model_config_id, mc.model_name, mc.display_name
            ORDER BY calls DESC
            LIMIT 5
            """)
    List<ModelUsageAggregate> topModelsByTenant(Long tenantId);

    @Select("""
            SELECT COALESCE(SUM(total_tokens), 0) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND usage_date >= #{startDate} AND usage_date <= #{endDate}
            """)
    Long sumTokensBetween(Long tenantId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT COUNT(*) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND usage_date >= #{startDate} AND usage_date <= #{endDate}
            """)
    Long countCallsBetween(Long tenantId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT COALESCE(SUM(cost), 0) FROM token_usage
            WHERE tenant_id = #{tenantId} AND currency = #{currency}
              AND usage_date >= #{startDate} AND usage_date <= #{endDate}
            """)
    BigDecimal sumCostBetween(Long tenantId, String currency, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT DATE_FORMAT(usage_date, '%m-%d') AS label,
                   COALESCE(SUM(total_tokens), 0) AS value
            FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND usage_date >= #{startDate} AND usage_date <= #{endDate}
            GROUP BY usage_date
            ORDER BY usage_date
            """)
    List<UsageTrendPoint> dailyTokenTrend(Long tenantId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT tu.usage_type AS usageType,
                   COUNT(*) AS calls,
                   COALESCE(SUM(tu.total_tokens), 0) AS tokens
            FROM token_usage tu
            WHERE tu.tenant_id = #{tenantId}
              AND tu.usage_date >= #{startDate} AND tu.usage_date <= #{endDate}
            GROUP BY tu.usage_type
            ORDER BY tokens DESC
            """)
    List<UsageTypeAggregate> usageByType(Long tenantId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT mc.model_name AS modelName,
                   mc.display_name AS displayName,
                   COUNT(*) AS calls,
                   COALESCE(SUM(tu.total_tokens), 0) AS tokens
            FROM token_usage tu
            LEFT JOIN model_config mc ON tu.model_config_id = mc.id
            WHERE tu.tenant_id = #{tenantId}
              AND tu.usage_date >= #{startDate} AND tu.usage_date <= #{endDate}
            GROUP BY tu.model_config_id, mc.model_name, mc.display_name
            ORDER BY tokens DESC
            LIMIT #{limit}
            """)
    List<ModelUsageAggregate> topModelsBetween(
            Long tenantId, LocalDate startDate, LocalDate endDate, int limit);

    @Select("""
            <script>
            SELECT tu.id, tu.agent_id, a.agent_name, mc.model_name, mc.display_name,
                   tu.usage_type, tu.input_tokens, tu.output_tokens, tu.total_tokens,
                   tu.cost, tu.currency, tu.latency_ms, tu.user_id, tu.created_at
            FROM token_usage tu
            LEFT JOIN agent a ON tu.agent_id = a.id
            LEFT JOIN model_config mc ON tu.model_config_id = mc.id
            WHERE tu.tenant_id = #{tenantId}
            <if test="agentId != null">AND tu.agent_id = #{agentId}</if>
            <if test="usageType != null and usageType != ''">AND tu.usage_type = #{usageType}</if>
            <if test="startDate != null">AND tu.usage_date &gt;= #{startDate}</if>
            <if test="endDate != null">AND tu.usage_date &lt;= #{endDate}</if>
            <if test="keyword != null and keyword != ''">
              AND (a.agent_name LIKE CONCAT('%', #{keyword}, '%')
                   OR mc.model_name LIKE CONCAT('%', #{keyword}, '%')
                   OR mc.display_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY tu.created_at DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<TokenUsageLogRow> pageLogs(
            Long tenantId,
            Long agentId,
            String usageType,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            int offset,
            int pageSize);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM token_usage tu
            LEFT JOIN agent a ON tu.agent_id = a.id
            LEFT JOIN model_config mc ON tu.model_config_id = mc.id
            WHERE tu.tenant_id = #{tenantId}
            <if test="agentId != null">AND tu.agent_id = #{agentId}</if>
            <if test="usageType != null and usageType != ''">AND tu.usage_type = #{usageType}</if>
            <if test="startDate != null">AND tu.usage_date &gt;= #{startDate}</if>
            <if test="endDate != null">AND tu.usage_date &lt;= #{endDate}</if>
            <if test="keyword != null and keyword != ''">
              AND (a.agent_name LIKE CONCAT('%', #{keyword}, '%')
                   OR mc.model_name LIKE CONCAT('%', #{keyword}, '%')
                   OR mc.display_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    Long countLogs(
            Long tenantId,
            Long agentId,
            String usageType,
            LocalDate startDate,
            LocalDate endDate,
            String keyword);
}

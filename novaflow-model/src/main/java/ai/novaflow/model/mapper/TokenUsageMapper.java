package ai.novaflow.model.mapper;

import ai.novaflow.model.domain.ModelUsageAggregate;
import ai.novaflow.model.entity.TokenUsageEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
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
}

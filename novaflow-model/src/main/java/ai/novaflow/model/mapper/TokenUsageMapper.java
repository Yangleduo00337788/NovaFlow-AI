package ai.novaflow.model.mapper;

import ai.novaflow.model.domain.CostAllocationAggregate;
import ai.novaflow.model.domain.ModelUsageAggregate;
import ai.novaflow.model.domain.TokenUsageLogRow;
import ai.novaflow.model.domain.UsageTrendPoint;
import ai.novaflow.model.domain.UsageTypeAggregate;
import ai.novaflow.model.entity.TokenUsageEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsageEntity> {

    Long countCallsByTenant(Long tenantId);

    Long sumTokensByTenant(Long tenantId);

    BigDecimal sumCostByTenantAndCurrency(Long tenantId, String currency);

    List<ModelUsageAggregate> topModelsByTenant(Long tenantId);

    Long sumTokensBetween(Long tenantId, LocalDate startDate, LocalDate endDate);

    Long sumTokensBetweenAllTenants(LocalDate startDate, LocalDate endDate);

    Long countCallsBetween(Long tenantId, LocalDate startDate, LocalDate endDate);

    BigDecimal sumCostBetween(Long tenantId, String currency, LocalDate startDate, LocalDate endDate);

    List<UsageTrendPoint> dailyTokenTrend(Long tenantId, LocalDate startDate, LocalDate endDate);

    List<UsageTypeAggregate> usageByType(Long tenantId, LocalDate startDate, LocalDate endDate);

    List<ModelUsageAggregate> topModelsBetween(
            Long tenantId, LocalDate startDate, LocalDate endDate, int limit);

    List<TokenUsageLogRow> pageLogs(
            Long tenantId,
            Long agentId,
            String usageType,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Integer success,
            int offset,
            int pageSize);

    List<TokenUsageLogRow> exportLogs(Long tenantId, LocalDate startDate, LocalDate endDate, int limit);

    Long countLogs(
            Long tenantId,
            Long agentId,
            String usageType,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Integer success);

    List<TokenUsageEntity> listNeedingCostBackfill(Long tenantId, int limit);

    List<CostAllocationAggregate> allocateByApplication(Long tenantId, LocalDate startDate, LocalDate endDate);

    List<CostAllocationAggregate> allocateByWorkspace(Long tenantId, LocalDate startDate, LocalDate endDate);

    List<CostAllocationAggregate> allocateByUser(Long tenantId, LocalDate startDate, LocalDate endDate);
}

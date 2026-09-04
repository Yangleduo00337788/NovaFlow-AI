package ai.novaflow.model.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.mapper.TokenUsageMapper;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantTokenQuotaGuardTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private TokenUsageMapper tokenUsageMapper;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @InjectMocks
    private TenantTokenQuotaGuard tenantTokenQuotaGuard;

    @Test
    void rejectsWhenReserveWouldExceedQuota() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(10L);
        tenant.setMonthlyTokenQuota(100L);
        when(tenantMapper.selectOneById(10L)).thenReturn(tenant);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(101L);

        assertThrows(BusinessException.class, () -> tenantTokenQuotaGuard.checkAndReserve(10L));
        verify(valueOperations).increment(anyString(), eq(-1L));
    }

    @Test
    void skipsWhenQuotaNotConfigured() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(10L);
        tenant.setMonthlyTokenQuota(0L);
        when(tenantMapper.selectOneById(10L)).thenReturn(tenant);

        tenantTokenQuotaGuard.checkAndReserve(10L);

        verify(stringRedisTemplate, never()).opsForValue();
    }
}

package ai.novaflow.user.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.user.entity.IpBlacklistEntity;
import ai.novaflow.user.mapper.IpBlacklistMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpBlacklistServiceTest {

    @Mock
    private IpBlacklistMapper ipBlacklistMapper;

    @Mock
    private PermissionService permissionService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private IpBlacklistService ipBlacklistService;

    @BeforeEach
    void setUp() {
        IpBlacklistEntity entity = new IpBlacklistEntity();
        entity.setIpAddress("203.0.113.10");
        entity.setStatus(1);
        entity.setIsDeleted(0);
        when(ipBlacklistMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(entity));
        ipBlacklistService.refreshCache();
    }

    @Test
    void blocksConfiguredIp() {
        assertTrue(ipBlacklistService.isBlocked("203.0.113.10"));
    }

    @Test
    void allowsOtherIp() {
        assertFalse(ipBlacklistService.isBlocked("198.51.100.1"));
    }

    @Test
    void requireAllowedThrowsForBlockedIp() {
        BusinessException ex = assertThrows(BusinessException.class, () -> ipBlacklistService.requireAllowed("203.0.113.10"));
        assertTrue(ex.getMessage().contains("IP 已被禁止访问"));
    }
}

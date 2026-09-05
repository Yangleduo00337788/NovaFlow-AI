package ai.novaflow.security.filter;

import ai.novaflow.common.security.AccountTypes;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAccessFilterTest {

    @InjectMocks
    private PlatformAccessFilter filter;

    private MockedStatic<StpUtil> stpUtil;

    @AfterEach
    void tearDown() {
        if (stpUtil != null) {
            stpUtil.close();
        }
    }

    @Test
    void blocksPlatformAccountFromTenantApi() throws Exception {
        stpUtil = mockStatic(StpUtil.class);
        stpUtil.when(StpUtil::isLogin).thenReturn(true);
        SaSession session = mock(SaSession.class);
        stpUtil.when(StpUtil::getSession).thenReturn(session);
        when(session.get("accountType")).thenReturn(AccountTypes.PLATFORM);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/agents");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsPlatformAccountOnPlatformApi() throws Exception {
        stpUtil = mockStatic(StpUtil.class);
        stpUtil.when(StpUtil::isLogin).thenReturn(true);
        SaSession session = mock(SaSession.class);
        stpUtil.when(StpUtil::getSession).thenReturn(session);
        when(session.get("accountType")).thenReturn(AccountTypes.PLATFORM);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/platform/tenants");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void blocksTenantAccountFromPlatformApi() throws Exception {
        stpUtil = mockStatic(StpUtil.class);
        stpUtil.when(StpUtil::isLogin).thenReturn(true);
        SaSession session = mock(SaSession.class);
        stpUtil.when(StpUtil::getSession).thenReturn(session);
        when(session.get("accountType")).thenReturn(AccountTypes.TENANT);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/platform/stats");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
}

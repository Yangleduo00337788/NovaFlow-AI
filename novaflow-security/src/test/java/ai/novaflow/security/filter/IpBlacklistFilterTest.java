package ai.novaflow.security.filter;

import ai.novaflow.common.security.IpBlacklistChecker;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpBlacklistFilterTest {

    @Test
    void blocksBlacklistedIp() throws Exception {
        IpBlacklistChecker checker = mock(IpBlacklistChecker.class);
        when(checker.isBlocked("1.2.3.4")).thenReturn(true);
        IpBlacklistFilter filter = new IpBlacklistFilter(checker);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsNonBlacklistedIp() throws Exception {
        IpBlacklistChecker checker = mock(IpBlacklistChecker.class);
        when(checker.isBlocked("8.8.8.8")).thenReturn(false);
        IpBlacklistFilter filter = new IpBlacklistFilter(checker);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/agents");
        request.setRemoteAddr("8.8.8.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}

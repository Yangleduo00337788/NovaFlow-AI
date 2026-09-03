package ai.novaflow.agent.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentDebugServiceTest {

    @Test
    void resolveChannelUsesPortalPrefix() {
        assertEquals("portal", AgentDebugService.resolveChannel("portal-12-171000"));
        assertEquals("debug", AgentDebugService.resolveChannel("debug-1"));
        assertEquals("debug", AgentDebugService.resolveChannel(null));
        assertEquals("debug", AgentDebugService.resolveChannel(""));
    }
}

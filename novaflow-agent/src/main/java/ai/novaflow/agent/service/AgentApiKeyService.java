package ai.novaflow.agent.service;

import ai.novaflow.agent.entity.AgentApiKeyEntity;
import ai.novaflow.agent.mapper.AgentApiKeyMapper;
import ai.novaflow.common.exception.BusinessException;import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class AgentApiKeyService {

    private static final String KEY_PREFIX = "nf_live_";

    private final AgentApiKeyMapper agentApiKeyMapper;

    @Transactional
    public String issueApiKey(Long agentId, Long tenantId) {
        String rawKey = KEY_PREFIX + RandomUtil.randomString(32);
        String hash = hash(rawKey);
        String prefix = rawKey.substring(0, Math.min(16, rawKey.length()));

        AgentApiKeyEntity existing = findByAgentId(agentId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            AgentApiKeyEntity entity = new AgentApiKeyEntity();
            entity.setTenantId(tenantId);
            entity.setAgentId(agentId);
            entity.setApiKeyHash(hash);
            entity.setApiKeyPrefix(prefix);
            entity.setStatus(1);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            agentApiKeyMapper.insert(entity);
        } else {
            existing.setApiKeyHash(hash);
            existing.setApiKeyPrefix(prefix);
            existing.setStatus(1);
            existing.setUpdatedAt(now);
            agentApiKeyMapper.update(existing);
        }
        return rawKey;
    }

    @Transactional
    public void disableApiKey(Long agentId) {
        AgentApiKeyEntity existing = findByAgentId(agentId);
        if (existing == null) {
            return;
        }
        existing.setStatus(0);
        existing.setUpdatedAt(LocalDateTime.now());
        agentApiKeyMapper.update(existing);
    }

    public AgentApiKeyEntity authenticate(Long agentId, String rawApiKey) {
        if (!StringUtils.hasText(rawApiKey)) {
            throw new BusinessException(40101, "缺少 API Key");
        }
        String hash = hash(rawApiKey.trim());
        AgentApiKeyEntity entity = agentApiKeyMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("api_key_hash", hash)
                        .eq("agent_id", agentId)
                        .eq("status", 1)
        );
        if (entity == null) {
            throw new BusinessException(40101, "API Key 无效");
        }
        entity.setLastUsedAt(LocalDateTime.now());
        agentApiKeyMapper.update(entity);
        return entity;
    }

    public AgentApiKeyEntity findByAgentId(Long agentId) {
        return agentApiKeyMapper.selectOneByQuery(
                QueryWrapper.create().eq("agent_id", agentId).limit(1)
        );
    }

    public String hash(String rawApiKey) {
        return SecureUtil.sha256(rawApiKey);
    }
}

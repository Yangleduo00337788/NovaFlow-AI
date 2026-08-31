package ai.novaflow.agent.service;

import ai.novaflow.agent.entity.AgentEmbedTokenEntity;
import ai.novaflow.agent.mapper.AgentEmbedTokenMapper;
import ai.novaflow.common.exception.BusinessException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgentEmbedTokenService {

    public static final String TOKEN_PREFIX = "nf_embed_";

    private final AgentEmbedTokenMapper agentEmbedTokenMapper;

    @Transactional
    public String issueEmbedToken(Long agentId, Long tenantId) {
        String rawToken = TOKEN_PREFIX + RandomUtil.randomString(32);
        String hash = hash(rawToken);
        String prefix = rawToken.substring(0, Math.min(20, rawToken.length()));

        AgentEmbedTokenEntity existing = findByAgentId(agentId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            AgentEmbedTokenEntity entity = new AgentEmbedTokenEntity();
            entity.setTenantId(tenantId);
            entity.setAgentId(agentId);
            entity.setTokenHash(hash);
            entity.setTokenPrefix(prefix);
            entity.setStatus(1);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            agentEmbedTokenMapper.insert(entity);
        } else {
            existing.setTokenHash(hash);
            existing.setTokenPrefix(prefix);
            existing.setStatus(1);
            existing.setUpdatedAt(now);
            agentEmbedTokenMapper.update(existing);
        }
        return rawToken;
    }

    @Transactional
    public void disableEmbedToken(Long agentId) {
        AgentEmbedTokenEntity existing = findByAgentId(agentId);
        if (existing == null) {
            return;
        }
        existing.setStatus(0);
        existing.setUpdatedAt(LocalDateTime.now());
        agentEmbedTokenMapper.update(existing);
    }

    public AgentEmbedTokenEntity authenticate(Long agentId, String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new BusinessException(40101, "缺少 Embed Token");
        }
        if (!rawToken.startsWith(TOKEN_PREFIX)) {
            throw new BusinessException(40101, "Embed Token 无效");
        }
        String hash = hash(rawToken.trim());
        AgentEmbedTokenEntity entity = agentEmbedTokenMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("token_hash", hash)
                        .eq("agent_id", agentId)
                        .eq("status", 1)
        );
        if (entity == null) {
            throw new BusinessException(40101, "Embed Token 无效");
        }
        entity.setLastUsedAt(LocalDateTime.now());
        agentEmbedTokenMapper.update(entity);
        return entity;
    }

    public AgentEmbedTokenEntity findByAgentId(Long agentId) {
        return agentEmbedTokenMapper.selectOneByQuery(
                QueryWrapper.create().eq("agent_id", agentId).limit(1)
        );
    }

    public String hash(String rawToken) {
        return SecureUtil.sha256(rawToken);
    }
}

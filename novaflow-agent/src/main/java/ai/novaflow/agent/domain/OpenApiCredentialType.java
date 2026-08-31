package ai.novaflow.agent.domain;

public enum OpenApiCredentialType {
    /** 服务端集成用，可列举指定终端用户的会话 */
    API_KEY,
    /** 嵌入页用，仅允许 welcome/chat/stream */
    EMBED_TOKEN
}

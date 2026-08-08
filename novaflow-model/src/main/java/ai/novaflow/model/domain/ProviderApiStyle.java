package ai.novaflow.model.domain;

public enum ProviderApiStyle {

  /** OpenAI 兼容 /v1/models 与 /v1/chat/completions */
  OPENAI_COMPATIBLE,
  /** 使用内置模型目录，不依赖上游 /models 列表 */
  CATALOG_ONLY
}

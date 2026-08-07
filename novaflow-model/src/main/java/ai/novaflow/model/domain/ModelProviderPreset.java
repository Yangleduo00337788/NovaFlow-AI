package ai.novaflow.model.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum ModelProviderPreset {

    OPENAI(
            "openai",
            "OpenAI",
            "https://api.openai.com/v1",
            "GPT 系列模型，适合通用对话与推理"
    ),
    DEEPSEEK(
            "deepseek",
            "DeepSeek",
            "https://api.deepseek.com/v1",
            "国产高性价比大模型"
    );

    private final String code;
    private final String name;
    private final String defaultBaseUrl;
    private final String description;

    public static Optional<ModelProviderPreset> of(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst();
    }
}

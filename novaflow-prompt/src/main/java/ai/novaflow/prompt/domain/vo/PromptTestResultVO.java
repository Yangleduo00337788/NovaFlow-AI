package ai.novaflow.prompt.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PromptTestResultVO {

    private String renderedPrompt;
    private String reply;
    private Integer tokensUsed;
    private Long latencyMs;
}

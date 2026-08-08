package ai.novaflow.tool.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolTestResultVO {

    private boolean success;
    private String result;
    private String error;
}

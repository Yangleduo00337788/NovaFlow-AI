package ai.novaflow.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEmbedConfig {

    /** 嵌入页主题色，如 #6366f1 */
    private String themeColor;

    /** 允许嵌入的父页面域名，如 example.com、*.example.com；空表示不限制 */
    @Builder.Default
    private List<String> allowedDomains = new ArrayList<>();

    /** postMessage 目标 Origin，默认 * */
    private String postMessageTargetOrigin;
}

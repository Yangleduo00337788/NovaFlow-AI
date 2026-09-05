package ai.novaflow.workflow.util;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.workflow.domain.WorkflowNodeType;
import ai.novaflow.workflow.entity.WorkflowEdgeEntity;
import ai.novaflow.workflow.entity.WorkflowNodeEntity;

import java.util.List;
import java.util.Map;

/**
 * 工作流发布前图结构校验（无起点、缺边等）。
 */
public final class WorkflowPublishValidator {

    private WorkflowPublishValidator() {
    }

    public static void validateGraph(List<WorkflowNodeEntity> nodes, List<WorkflowEdgeEntity> edges) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException("发布失败：请至少添加一个节点");
        }
        long startCount = nodes.stream().filter(node -> WorkflowNodeType.START.equals(node.getNodeType())).count();
        long endCount = nodes.stream().filter(node -> WorkflowNodeType.END.equals(node.getNodeType())).count();
        if (startCount != 1) {
            throw new BusinessException("发布失败：工作流需要且仅需要一个开始节点");
        }
        if (endCount < 1) {
            throw new BusinessException("发布失败：工作流至少需要一个结束节点");
        }
        if (edges == null || edges.isEmpty()) {
            throw new BusinessException("发布失败：请连接节点后再发布");
        }
    }

    public static void validateNodeConfigs(
            List<WorkflowNodeEntity> nodes,
            java.util.function.Function<String, Map<String, Object>> configParser) {
        for (WorkflowNodeEntity node : nodes) {
            Map<String, Object> config = configParser.apply(node.getNodeConfig());
            if (WorkflowNodeType.LLM.equals(node.getNodeType()) && toLong(config.get("modelConfigId")) == null) {
                throw new BusinessException("发布失败：LLM 节点「" + node.getNodeName() + "」未配置模型");
            }
            if (WorkflowNodeType.TOOL.equals(node.getNodeType()) && toLong(config.get("toolId")) == null) {
                throw new BusinessException("发布失败：工具节点「" + node.getNodeName() + "」未选择工具");
            }
            if (WorkflowNodeType.KNOWLEDGE.equals(node.getNodeType()) && toLong(config.get("knowledgeBaseId")) == null) {
                throw new BusinessException("发布失败：知识库节点「" + node.getNodeName() + "」未选择知识库");
            }
        }
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

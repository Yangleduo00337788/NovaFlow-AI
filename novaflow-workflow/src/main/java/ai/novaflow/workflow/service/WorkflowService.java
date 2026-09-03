package ai.novaflow.workflow.service;

import ai.novaflow.common.audit.AuditRecorder;
import ai.novaflow.common.application.ApplicationLookup;
import ai.novaflow.common.security.ResourceTypes;
import ai.novaflow.user.service.RecentAccessService;
import ai.novaflow.user.service.ResourceAccessService;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.util.PageQueryUtils;
import ai.novaflow.workflow.domain.WorkflowNodeType;
import ai.novaflow.workflow.domain.WorkflowStatus;
import ai.novaflow.workflow.domain.dto.WorkflowSaveRequest;
import ai.novaflow.workflow.domain.vo.WorkflowDetailVO;
import ai.novaflow.workflow.domain.vo.WorkflowEdgeVO;
import ai.novaflow.workflow.domain.vo.WorkflowNodeVO;
import ai.novaflow.workflow.domain.vo.WorkflowVO;
import ai.novaflow.workflow.entity.WorkflowEdgeEntity;
import ai.novaflow.workflow.entity.WorkflowEntity;
import ai.novaflow.workflow.entity.WorkflowNodeEntity;
import ai.novaflow.workflow.mapper.WorkflowEdgeMapper;
import ai.novaflow.workflow.mapper.WorkflowMapper;
import ai.novaflow.workflow.mapper.WorkflowNodeMapper;
import ai.novaflow.workflow.util.WorkflowElBuilder;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private static final int MAX_EDGE_ID_LENGTH = 64;

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper workflowNodeMapper;
    private final WorkflowEdgeMapper workflowEdgeMapper;
    private final ApplicationLookup applicationLookup;
    private final RecentAccessService recentAccessService;
    private final ResourceAccessService resourceAccessService;
    private final ObjectMapper objectMapper;
    private final AuditRecorder auditRecorder;

    public PageResult<WorkflowVO> page(int page, int pageSize, String keyword, Long applicationId) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            query.and("(workflow_name like ? or description like ?)",
                    "%" + keyword + "%", "%" + keyword + "%");
        }
        if (applicationId != null) {
            query.eq("application_id", applicationId);
        }
        query.orderBy("updated_at", false);

        Page<WorkflowEntity> result = workflowMapper.paginate(Page.of(page, pageSize), query);
        long userId = StpUtil.getLoginIdAsLong();
        Map<Long, String> appNameMap = buildApplicationNameMap(
                result.getRecords().stream().map(WorkflowEntity::getApplicationId).distinct().toList());
        List<WorkflowVO> list = result.getRecords().stream()
                .filter(entity -> resourceAccessService.canAccessResource(
                        userId, tenantId, ResourceTypes.WORKFLOW, entity.getId(), "workflow:read"))
                .map(entity -> toVO(entity, appNameMap.get(entity.getApplicationId()), countNodes(entity.getId())))
                .toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    public List<WorkflowVO> listPublishedOptions(Long applicationId) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0)
                .eq("status", WorkflowStatus.PUBLISHED);
        if (applicationId != null) {
            query.eq("application_id", applicationId);
        }
        query.orderBy("updated_at", false);
        return workflowMapper.selectListByQuery(query).stream()
                .map(entity -> toVO(entity, resolveApplicationName(entity.getApplicationId()), countNodes(entity.getId())))
                .toList();
    }

    public WorkflowEntity requirePublishedWorkflow(Long workflowId, Long tenantId) {
        WorkflowEntity entity = workflowMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", workflowId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("工作流不存在");
        }
        if (!Objects.equals(entity.getStatus(), WorkflowStatus.PUBLISHED)) {
            throw new BusinessException("工作流未发布，请先发布工作流");
        }
        return entity;
    }

    public WorkflowEntity requireWorkflow(Long workflowId, Long tenantId) {
        WorkflowEntity entity = workflowMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", workflowId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("工作流不存在");
        }
        return entity;
    }

    public WorkflowDetailVO detail(Long id) {
        resourceAccessService.requireResourceAccess(
                StpUtil.getLoginIdAsLong(), requireTenantId(), ResourceTypes.WORKFLOW, id, "workflow:read");
        WorkflowEntity entity = getWorkflowOrThrow(id);
        recordRecentAccess(entity);
        List<WorkflowNodeEntity> nodes = listNodes(id);
        List<WorkflowEdgeEntity> edges = listEdges(id);
        String appName = resolveApplicationName(entity.getApplicationId());
        return WorkflowDetailVO.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .applicationName(appName)
                .workflowName(entity.getWorkflowName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .elExpression(entity.getElExpression())
                .canvasData(parseCanvasData(entity.getCanvasData()))
                .nodes(nodes.stream().map(this::toNodeVO).toList())
                .edges(edges.stream().map(this::toEdgeVO).toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Transactional
    public WorkflowDetailVO create(WorkflowSaveRequest request) {
        Long tenantId = requireTenantId();
        ensureApplicationExists(request.getApplicationId());
        ensureNameUnique(tenantId, request.getWorkflowName(), null);

        LocalDateTime now = LocalDateTime.now();
        WorkflowEntity entity = new WorkflowEntity();
        entity.setTenantId(tenantId);
        entity.setApplicationId(request.getApplicationId());
        entity.setWorkflowName(request.getWorkflowName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setStatus(WorkflowStatus.DRAFT);
        entity.setVersion(1);
        entity.setCreatedBy(StpUtil.getLoginIdAsLong());
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        workflowMapper.insert(entity);

        syncCanvas(entity, request, now);
        return detail(entity.getId());
    }

    @Transactional
    public WorkflowDetailVO update(Long id, WorkflowSaveRequest request) {
        resourceAccessService.requireResourceAccess(
                StpUtil.getLoginIdAsLong(), requireTenantId(), ResourceTypes.WORKFLOW, id, "workflow:edit");
        WorkflowEntity entity = getWorkflowOrThrow(id);
        ensureApplicationExists(request.getApplicationId());
        ensureNameUnique(entity.getTenantId(), request.getWorkflowName(), id);

        entity.setApplicationId(request.getApplicationId());
        entity.setWorkflowName(request.getWorkflowName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setUpdatedAt(LocalDateTime.now());
        workflowMapper.update(entity);

        syncCanvas(entity, request, LocalDateTime.now());
        return detail(id);
    }

    @Transactional
    public WorkflowDetailVO publish(Long id) {
        resourceAccessService.requireResourceAccess(
                StpUtil.getLoginIdAsLong(), requireTenantId(), ResourceTypes.WORKFLOW, id, "workflow:publish");
        WorkflowEntity entity = getWorkflowOrThrow(id);
        List<WorkflowNodeEntity> nodes = listNodes(id);
        List<WorkflowEdgeEntity> edges = listEdges(id);
        validateForPublish(nodes, edges);

        entity.setStatus(WorkflowStatus.PUBLISHED);
        entity.setVersion(entity.getVersion() + 1);
        entity.setElExpression(WorkflowElBuilder.build(nodes, edges));
        entity.setUpdatedAt(LocalDateTime.now());
        workflowMapper.update(entity);
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        resourceAccessService.requireResourceAccess(
                StpUtil.getLoginIdAsLong(), requireTenantId(), ResourceTypes.WORKFLOW, id, "workflow:delete");
        WorkflowEntity entity = getWorkflowOrThrow(id);
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        workflowMapper.update(entity);
        auditRecorder.record("workflow.delete", "workflow", entity.getId(), "删除工作流: " + entity.getWorkflowName());
    }

    private void syncCanvas(WorkflowEntity entity, WorkflowSaveRequest request, LocalDateTime now) {
        workflowNodeMapper.deleteByQuery(QueryWrapper.create().eq("workflow_id", entity.getId()));
        workflowEdgeMapper.deleteByQuery(QueryWrapper.create().eq("workflow_id", entity.getId()));

        WorkflowSaveRequest.WorkflowCanvasData canvasData = request.getCanvasData();
        if (canvasData == null) {
            entity.setCanvasData(null);
            entity.setElExpression(null);
            workflowMapper.update(entity);
            return;
        }

        List<WorkflowNodeEntity> nodes = List.of();
        if (canvasData.getNodes() != null) {
            int order = 0;
            for (WorkflowSaveRequest.WorkflowCanvasNode node : canvasData.getNodes()) {
                if (!StringUtils.hasText(node.getId()) || !StringUtils.hasText(node.getType())) {
                    continue;
                }
                WorkflowNodeEntity nodeEntity = new WorkflowNodeEntity();
                nodeEntity.setTenantId(entity.getTenantId());
                nodeEntity.setWorkflowId(entity.getId());
                nodeEntity.setNodeId(node.getId());
                nodeEntity.setNodeType(node.getType());
                nodeEntity.setNodeName(resolveNodeName(node));
                nodeEntity.setPositionX(toDecimal(node.getPosition() != null ? node.getPosition().getX() : 0D));
                nodeEntity.setPositionY(toDecimal(node.getPosition() != null ? node.getPosition().getY() : 0D));
                nodeEntity.setNodeConfig(serializeConfig(node.getData() != null ? node.getData().getConfig() : null));
                nodeEntity.setSortOrder(order++);
                nodeEntity.setCreatedAt(now);
                nodeEntity.setUpdatedAt(now);
                workflowNodeMapper.insert(nodeEntity);
            }
            nodes = listNodes(entity.getId());
        }

        if (canvasData.getEdges() != null) {
            for (WorkflowSaveRequest.WorkflowCanvasEdge edge : canvasData.getEdges()) {
                if (!StringUtils.hasText(edge.getId()) || !StringUtils.hasText(edge.getSource()) || !StringUtils.hasText(edge.getTarget())) {
                    continue;
                }
                String edgeId = normalizeEdgeId(edge.getId());
                edge.setId(edgeId);
                WorkflowEdgeEntity edgeEntity = new WorkflowEdgeEntity();
                edgeEntity.setTenantId(entity.getTenantId());
                edgeEntity.setWorkflowId(entity.getId());
                edgeEntity.setEdgeId(edgeId);
                edgeEntity.setSourceNodeId(edge.getSource());
                edgeEntity.setTargetNodeId(edge.getTarget());
                edgeEntity.setSourceHandle(edge.getSourceHandle());
                edgeEntity.setTargetHandle(edge.getTargetHandle());
                edgeEntity.setCondition(edge.getLabel());
                edgeEntity.setCreatedAt(now);
                workflowEdgeMapper.insert(edgeEntity);
            }
        }

        List<WorkflowEdgeEntity> edges = listEdges(entity.getId());
        entity.setCanvasData(serializeCanvas(canvasData));
        entity.setElExpression(WorkflowElBuilder.build(nodes, edges));
        workflowMapper.update(entity);
    }

    private void validateForPublish(List<WorkflowNodeEntity> nodes, List<WorkflowEdgeEntity> edges) {
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
        for (WorkflowNodeEntity node : nodes) {
            Map<String, Object> config = parseConfig(node.getNodeConfig());
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

    private Long toLong(Object value) {
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

    private WorkflowEntity getWorkflowOrThrow(Long id) {
        WorkflowEntity entity = workflowMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", requireTenantId())
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("工作流不存在");
        }
        return entity;
    }

    private List<WorkflowNodeEntity> listNodes(Long workflowId) {
        return workflowNodeMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("workflow_id", workflowId)
                        .eq("tenant_id", requireTenantId())
                        .orderBy("sort_order", true)
        );
    }

    private List<WorkflowEdgeEntity> listEdges(Long workflowId) {
        return workflowEdgeMapper.selectListByQuery(
                QueryWrapper.create().eq("workflow_id", workflowId).eq("tenant_id", requireTenantId())
        );
    }

    private int countNodes(Long workflowId) {
        return (int) workflowNodeMapper.selectCountByQuery(
                QueryWrapper.create().eq("workflow_id", workflowId).eq("tenant_id", requireTenantId())
        );
    }

    private void ensureApplicationExists(Long applicationId) {
        applicationLookup.requireExists(requireTenantId(), applicationId);
    }

    private void ensureNameUnique(Long tenantId, String workflowName, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("workflow_name", workflowName.trim())
                .eq("is_deleted", 0);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        if (workflowMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("工作流名称已存在");
        }
    }

    private Map<Long, String> buildApplicationNameMap(List<Long> applicationIds) {
        return applicationLookup.getApplicationNameMap(applicationIds);
    }

    private String resolveApplicationName(Long applicationId) {
        return buildApplicationNameMap(List.of(applicationId)).getOrDefault(applicationId, "未知应用");
    }

    private WorkflowVO toVO(WorkflowEntity entity, String applicationName, int nodeCount) {
        return WorkflowVO.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .applicationName(applicationName)
                .workflowName(entity.getWorkflowName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .nodeCount(nodeCount)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private WorkflowNodeVO toNodeVO(WorkflowNodeEntity entity) {
        return WorkflowNodeVO.builder()
                .nodeId(entity.getNodeId())
                .nodeType(entity.getNodeType())
                .nodeName(entity.getNodeName())
                .positionX(entity.getPositionX() != null ? entity.getPositionX().doubleValue() : 0D)
                .positionY(entity.getPositionY() != null ? entity.getPositionY().doubleValue() : 0D)
                .nodeConfig(parseConfig(entity.getNodeConfig()))
                .build();
    }

    private WorkflowEdgeVO toEdgeVO(WorkflowEdgeEntity entity) {
        return WorkflowEdgeVO.builder()
                .edgeId(entity.getEdgeId())
                .sourceNodeId(entity.getSourceNodeId())
                .targetNodeId(entity.getTargetNodeId())
                .sourceHandle(entity.getSourceHandle())
                .targetHandle(entity.getTargetHandle())
                .condition(entity.getCondition())
                .build();
    }

    private String resolveNodeName(WorkflowSaveRequest.WorkflowCanvasNode node) {
        if (node.getData() != null && StringUtils.hasText(node.getData().getLabel())) {
            return node.getData().getLabel().trim();
        }
        return node.getType();
    }

    private BigDecimal toDecimal(Double value) {
        return BigDecimal.valueOf(value != null ? value : 0D);
    }

    private String serializeCanvas(WorkflowSaveRequest.WorkflowCanvasData canvasData) {
        try {
            return objectMapper.writeValueAsString(canvasData);
        } catch (Exception e) {
            throw new BusinessException("画布数据序列化失败");
        }
    }

    private Map<String, Object> parseCanvasData(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String serializeConfig(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config != null ? config : Map.of());
        } catch (Exception e) {
            throw new BusinessException("节点配置序列化失败");
        }
    }

    private Map<String, Object> parseConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String normalizeEdgeId(String edgeId) {
        String trimmed = edgeId.trim();
        if (trimmed.length() <= MAX_EDGE_ID_LENGTH) {
            return trimmed;
        }
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String generated = "e-" + System.currentTimeMillis() + "-" + suffix;
        return generated.length() <= MAX_EDGE_ID_LENGTH ? generated : generated.substring(0, MAX_EDGE_ID_LENGTH);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void recordRecentAccess(WorkflowEntity entity) {
        if (!StpUtil.isLogin()) {
            return;
        }
        recentAccessService.record(
                entity.getTenantId(),
                StpUtil.getLoginIdAsLong(),
                "workflow",
                entity.getId(),
                entity.getWorkflowName()
        );
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}

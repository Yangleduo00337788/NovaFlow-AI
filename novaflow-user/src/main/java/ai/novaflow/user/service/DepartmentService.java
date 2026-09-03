package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.tenant.entity.DepartmentEntity;
import ai.novaflow.tenant.mapper.DepartmentMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.user.domain.dto.DepartmentSaveRequest;
import ai.novaflow.user.domain.vo.DepartmentVO;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private static final int MAX_DEPARTMENTS = 200;

    private final DepartmentMapper departmentMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final AuditLogService auditLogService;
    private final PermissionService permissionService;

    public List<DepartmentVO> listTree() {
        Long tenantId = requireTenantId();
        requireOrgManage();
        List<DepartmentEntity> entities = listEntities(tenantId);
        Map<Long, Long> memberCounts = loadMemberCounts(tenantId);
        Map<Long, DepartmentVO> nodes = new HashMap<>();
        for (DepartmentEntity entity : entities) {
            nodes.put(entity.getId(), toVo(entity, memberCounts.getOrDefault(entity.getId(), 0L)));
        }
        List<DepartmentVO> roots = new ArrayList<>();
        for (DepartmentEntity entity : entities) {
            DepartmentVO node = nodes.get(entity.getId());
            DepartmentVO parent = entity.getParentId() == null ? null : nodes.get(entity.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortRecursively(roots);
        return roots;
    }

    @Transactional
    public DepartmentVO create(DepartmentSaveRequest request) {
        Long tenantId = requireTenantId();
        requireOrgManage();
        String name = request.getDeptName().trim();
        Long parentId = normalizeParentId(request.getParentId());
        if (countDepartments(tenantId) >= MAX_DEPARTMENTS) {
            throw new BusinessException("部门数量已达上限（" + MAX_DEPARTMENTS + "）");
        }
        Map<Long, Long> parentById = parentMap(tenantId);
        if (parentId != null) {
            requireDepartment(parentId, tenantId);
            if (DepartmentHierarchy.depthOf(parentId, parentById) + 1 > DepartmentHierarchy.MAX_DEPTH) {
                throw new BusinessException("部门层级不能超过 " + DepartmentHierarchy.MAX_DEPTH + " 层");
            }
        }
        ensureNameUnique(tenantId, parentId, name, null);

        LocalDateTime now = LocalDateTime.now();
        DepartmentEntity entity = new DepartmentEntity();
        entity.setTenantId(tenantId);
        entity.setParentId(parentId);
        entity.setDeptName(name);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        departmentMapper.insert(entity);
        auditLogService.record("department.create", "department", entity.getId(), "创建部门: " + name);
        return toVo(entity, 0L);
    }

    @Transactional
    public DepartmentVO update(Long id, DepartmentSaveRequest request) {
        Long tenantId = requireTenantId();
        requireOrgManage();
        DepartmentEntity entity = requireDepartment(id, tenantId);
        String name = request.getDeptName().trim();
        Long parentId = normalizeParentId(request.getParentId());
        Map<Long, Long> parentById = parentMap(tenantId);
        if (parentId != null) {
            requireDepartment(parentId, tenantId);
            if (DepartmentHierarchy.isSelfOrDescendant(id, parentId, parentById)) {
                throw new BusinessException("不能将部门移动到自身或下级部门下");
            }
            if (DepartmentHierarchy.depthOf(parentId, parentById) + 1 > DepartmentHierarchy.MAX_DEPTH) {
                throw new BusinessException("部门层级不能超过 " + DepartmentHierarchy.MAX_DEPTH + " 层");
            }
        }
        ensureNameUnique(tenantId, parentId, name, id);

        entity.setDeptName(name);
        entity.setParentId(parentId);
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        departmentMapper.updateStructure(
                entity.getId(),
                tenantId,
                entity.getDeptName(),
                entity.getParentId(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder());
        auditLogService.record("department.update", "department", entity.getId(), "更新部门: " + name);
        long memberCount = tenantMemberMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("department_id", id)
                        .eq("is_deleted", 0));
        return toVo(entity, memberCount);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        requireOrgManage();
        DepartmentEntity entity = requireDepartment(id, tenantId);
        long childCount = departmentMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("parent_id", id)
                        .eq("is_deleted", 0));
        if (childCount > 0) {
            throw new BusinessException("请先删除下级部门");
        }
        tenantMemberMapper.clearDepartment(tenantId, id);
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        departmentMapper.update(entity);
        auditLogService.record("department.delete", "department", entity.getId(), "删除部门: " + entity.getDeptName());
    }

    DepartmentEntity requireDepartment(Long id, Long tenantId) {
        DepartmentEntity entity = departmentMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .limit(1));
        if (entity == null) {
            throw new BusinessException("部门不存在");
        }
        return entity;
    }

    private List<DepartmentEntity> listEntities(Long tenantId) {
        return departmentMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .orderBy("sort_order", true)
                        .orderBy("id", true));
    }

    private Map<Long, Long> parentMap(Long tenantId) {
        Map<Long, Long> map = new HashMap<>();
        for (DepartmentEntity entity : listEntities(tenantId)) {
            map.put(entity.getId(), entity.getParentId());
        }
        return map;
    }

    private Map<Long, Long> loadMemberCounts(Long tenantId) {
        List<ai.novaflow.tenant.entity.TenantMemberEntity> members = tenantMemberMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .isNotNull("department_id"));
        Map<Long, Long> counts = new HashMap<>();
        for (var member : members) {
            if (member.getDepartmentId() != null) {
                counts.merge(member.getDepartmentId(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private void ensureNameUnique(Long tenantId, Long parentId, String name, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("dept_name", name)
                .eq("is_deleted", 0);
        if (parentId == null) {
            query.isNull("parent_id");
        } else {
            query.eq("parent_id", parentId);
        }
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        if (departmentMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("同级部门名称已存在");
        }
    }

    private long countDepartments(Long tenantId) {
        return departmentMapper.selectCountByQuery(
                QueryWrapper.create().eq("tenant_id", tenantId).eq("is_deleted", 0));
    }

    private Long normalizeParentId(Long parentId) {
        if (parentId == null || parentId <= 0) {
            return null;
        }
        return parentId;
    }

    private void sortRecursively(List<DepartmentVO> nodes) {
        nodes.sort(Comparator
                .comparing((DepartmentVO item) -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                .thenComparing(DepartmentVO::getId));
        for (DepartmentVO node : nodes) {
            sortRecursively(node.getChildren());
        }
    }

    private DepartmentVO toVo(DepartmentEntity entity, long memberCount) {
        return DepartmentVO.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .deptName(entity.getDeptName())
                .sortOrder(entity.getSortOrder())
                .memberCount(memberCount)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .children(new ArrayList<>())
                .build();
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("未获取到租户上下文");
        }
        return tenantId;
    }

    private void requireOrgManage() {
        permissionService.requireAnyPermission(
                cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong(),
                requireTenantId(),
                "member:manage",
                "tenant:manage");
    }
}

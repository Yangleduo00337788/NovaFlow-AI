package ai.novaflow.tenant.mapper;

import ai.novaflow.tenant.entity.TenantMemberEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TenantMemberMapper extends BaseMapper<TenantMemberEntity> {

    @Update("""
            UPDATE tenant_member
            SET department_id = #{departmentId}, updated_at = NOW()
            WHERE id = #{id} AND tenant_id = #{tenantId} AND is_deleted = 0
            """)
    int updateDepartmentId(
            @Param("id") Long id,
            @Param("tenantId") Long tenantId,
            @Param("departmentId") Long departmentId);

    @Update("""
            UPDATE tenant_member
            SET department_id = NULL, updated_at = NOW()
            WHERE tenant_id = #{tenantId} AND department_id = #{departmentId} AND is_deleted = 0
            """)
    int clearDepartment(
            @Param("tenantId") Long tenantId,
            @Param("departmentId") Long departmentId);
}

package ai.novaflow.tenant.mapper;

import ai.novaflow.tenant.entity.DepartmentEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DepartmentMapper extends BaseMapper<DepartmentEntity> {

    @Update("""
            UPDATE department
            SET dept_name = #{deptName}, parent_id = #{parentId}, sort_order = #{sortOrder}, updated_at = NOW()
            WHERE id = #{id} AND tenant_id = #{tenantId} AND is_deleted = 0
            """)
    int updateStructure(
            @Param("id") Long id,
            @Param("tenantId") Long tenantId,
            @Param("deptName") String deptName,
            @Param("parentId") Long parentId,
            @Param("sortOrder") Integer sortOrder);
}

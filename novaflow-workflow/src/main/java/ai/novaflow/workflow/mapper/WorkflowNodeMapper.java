package ai.novaflow.workflow.mapper;

import ai.novaflow.workflow.domain.WorkflowNodeCountRow;
import ai.novaflow.workflow.entity.WorkflowNodeEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNodeEntity> {

    @Select("""
            <script>
            SELECT workflow_id AS workflowId, COUNT(*) AS nodeCount
            FROM workflow_node
            WHERE tenant_id = #{tenantId}
              AND workflow_id IN
              <foreach collection="workflowIds" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            GROUP BY workflow_id
            </script>
            """)
    List<WorkflowNodeCountRow> countByWorkflowIds(
            @Param("tenantId") Long tenantId,
            @Param("workflowIds") List<Long> workflowIds);
}

package ai.novaflow.workflow.mapper;

import ai.novaflow.workflow.entity.WorkflowExecutionEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowExecutionMapper extends BaseMapper<WorkflowExecutionEntity> {
}

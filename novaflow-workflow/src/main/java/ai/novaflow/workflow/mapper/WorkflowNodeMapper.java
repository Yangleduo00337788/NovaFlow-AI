package ai.novaflow.workflow.mapper;

import ai.novaflow.workflow.entity.WorkflowNodeEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNodeEntity> {
}

package ai.novaflow.workflow.mapper;

import ai.novaflow.workflow.entity.WorkflowEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowMapper extends BaseMapper<WorkflowEntity> {
}

package ai.novaflow.application.mapper;

import ai.novaflow.application.entity.ApplicationEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApplicationMapper extends BaseMapper<ApplicationEntity> {
}

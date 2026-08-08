package ai.novaflow.knowledge.mapper;

import ai.novaflow.knowledge.entity.DocumentEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {
}

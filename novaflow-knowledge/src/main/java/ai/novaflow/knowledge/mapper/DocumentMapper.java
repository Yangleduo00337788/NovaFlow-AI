package ai.novaflow.knowledge.mapper;

import ai.novaflow.knowledge.entity.DocumentEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {

    /**
     * 汇总租户现存文档的总字节数（未删除文档），用于存储配额校验。
     */
    Long sumFileSizeByTenant(Long tenantId);
}

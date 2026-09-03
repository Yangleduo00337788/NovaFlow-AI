package ai.novaflow.agent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 门户访问校验用的只读查询：避免 agent 模块反向依赖 application 模块。
 */
@Mapper
public interface ApplicationAccessMapper {

    @Select("""
            SELECT COUNT(1) FROM application
            WHERE id = #{applicationId}
              AND tenant_id = #{tenantId}
              AND publish_status = 1
              AND status = 1
              AND is_deleted = 0
            """)
    int countPublishedApp(@Param("applicationId") Long applicationId, @Param("tenantId") Long tenantId);
}

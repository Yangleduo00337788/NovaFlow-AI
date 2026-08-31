package ai.novaflow.tenant.mapper;

import ai.novaflow.tenant.entity.TenantMemberEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMemberMapper extends BaseMapper<TenantMemberEntity> {
}

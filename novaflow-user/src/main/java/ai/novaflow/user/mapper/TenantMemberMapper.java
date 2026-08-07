package ai.novaflow.user.mapper;

import ai.novaflow.user.entity.TenantMemberEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMemberMapper extends BaseMapper<TenantMemberEntity> {
}

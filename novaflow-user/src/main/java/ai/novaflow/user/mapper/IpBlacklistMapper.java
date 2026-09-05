package ai.novaflow.user.mapper;

import ai.novaflow.user.entity.IpBlacklistEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IpBlacklistMapper extends BaseMapper<IpBlacklistEntity> {
}

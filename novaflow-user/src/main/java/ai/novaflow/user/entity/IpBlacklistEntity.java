package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("ip_blacklist")
public class IpBlacklistEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String ipAddress;
    private String reason;
    private Integer status;
    private LocalDateTime expireAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}

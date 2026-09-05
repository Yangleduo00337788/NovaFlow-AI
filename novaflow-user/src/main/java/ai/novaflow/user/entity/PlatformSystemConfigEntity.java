package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("platform_system_config")
public class PlatformSystemConfigEntity {

    @Id
    private String configKey;
    private String configValue;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}

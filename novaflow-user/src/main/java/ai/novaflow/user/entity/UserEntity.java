package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("user")
public class UserEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String username;
    private String email;
    @Column("password_hash")
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String accountType;
    @Column("platform_role_code")
    private String platformRoleCode;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}

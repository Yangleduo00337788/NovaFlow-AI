package ai.novaflow.user.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ResourcePermissionSaveRequest {

    @Valid
    private List<GrantItem> grants;

    @Data
    public static class GrantItem {
        @NotNull
        private Long userId;
        @NotBlank
        private String permissionCode;
    }
}

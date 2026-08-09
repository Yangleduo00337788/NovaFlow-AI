package ai.novaflow.dashboard.domain.dto;

import lombok.Data;

@Data
public class FavoriteToggleRequest {

    private String resourceType;
    private Long resourceId;
    private String resourceName;
}

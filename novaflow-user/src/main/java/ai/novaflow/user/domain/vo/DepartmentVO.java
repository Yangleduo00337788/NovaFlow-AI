package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class DepartmentVO {

    private Long id;
    private Long parentId;
    private String deptName;
    private Integer sortOrder;
    private long memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Builder.Default
    private List<DepartmentVO> children = new ArrayList<>();
}

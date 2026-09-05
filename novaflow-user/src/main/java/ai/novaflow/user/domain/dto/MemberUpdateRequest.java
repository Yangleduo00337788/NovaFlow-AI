package ai.novaflow.user.domain.dto;

import lombok.Data;

@Data
public class MemberUpdateRequest {

    private String roleCode;

    private Integer status;

    private Long departmentId;
}

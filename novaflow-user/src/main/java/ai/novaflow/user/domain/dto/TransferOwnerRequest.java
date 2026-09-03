package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.NotNull;

public class TransferOwnerRequest {

    @NotNull(message = "成员 ID 不能为空")
    private Long memberId;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
}

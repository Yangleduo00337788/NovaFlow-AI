package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserNotificationVO {

    private Long id;
    private String category;
    private String title;
    private String content;
    private String linkUrl;
    private Boolean read;
    private LocalDateTime createdAt;
}

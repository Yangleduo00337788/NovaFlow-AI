package ai.novaflow.application.service;

import ai.novaflow.application.mapper.ApplicationMapper;
import ai.novaflow.common.application.ApplicationWorkspaceChecker;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationWorkspaceCheckerImpl implements ApplicationWorkspaceChecker {

    private final ApplicationMapper applicationMapper;

    @Override
    public long countByWorkspace(Long workspaceId) {
        return applicationMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("workspace_id", workspaceId)
                        .eq("is_deleted", 0)
        );
    }
}

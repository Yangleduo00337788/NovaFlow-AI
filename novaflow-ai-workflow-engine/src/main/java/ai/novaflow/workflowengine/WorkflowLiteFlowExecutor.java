package ai.novaflow.workflowengine;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.workflowengine.domain.WorkflowExecutionContext;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WorkflowLiteFlowExecutor {

    private final FlowExecutor flowExecutor;

    public WorkflowExecutionContext execute(String chainId, String elExpression, WorkflowExecutionContext context) {
        if (!StringUtils.hasText(elExpression)) {
            throw new BusinessException("工作流 EL 表达式为空，无法执行");
        }
        LiteFlowChainELBuilder.createChain()
                .setChainName(chainId)
                .setEL(elExpression)
                .build();
        try {
            LiteflowResponse response = flowExecutor.execute2Resp(chainId, null, context);
            if (!response.isSuccess() && !context.isFailed()) {
                String message = response.getMessage() != null ? response.getMessage() : "LiteFlow 执行失败";
                context.setFailed(true);
                context.setErrorMessage(message);
            }
            return context;
        } finally {
            FlowBus.removeChain(chainId);
        }
    }
}

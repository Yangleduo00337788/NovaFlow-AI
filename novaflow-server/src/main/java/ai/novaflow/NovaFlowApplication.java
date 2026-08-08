package ai.novaflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "ai.novaflow")
@MapperScan({"ai.novaflow.user.mapper", "ai.novaflow.agent.mapper", "ai.novaflow.model.mapper", "ai.novaflow.knowledge.mapper", "ai.novaflow.dashboard.mapper", "ai.novaflow.tool.mapper", "ai.novaflow.prompt.mapper"})
@EnableAsync
public class NovaFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovaFlowApplication.class, args);
    }
}

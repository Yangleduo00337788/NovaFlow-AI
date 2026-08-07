package ai.novaflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ai.novaflow")
@MapperScan({"ai.novaflow.user.mapper", "ai.novaflow.agent.mapper", "ai.novaflow.model.mapper"})
public class NovaFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovaFlowApplication.class, args);
    }
}

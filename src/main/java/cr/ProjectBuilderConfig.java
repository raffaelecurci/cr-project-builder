package cr;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@RefreshScope
@Configuration
public class ProjectBuilderConfig {
	
	@Bean("cr-rabbit-executor")
    public TaskExecutor taskExecutor() {
//		return new ThreadPoolTaskExecutor();
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        return executor;
	}
}

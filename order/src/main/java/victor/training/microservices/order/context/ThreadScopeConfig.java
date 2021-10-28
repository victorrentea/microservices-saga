package victor.training.microservices.order.context;

import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadScopeConfig {
   @Bean
   public static CustomScopeConfigurer defineThreadScope() {
      CustomScopeConfigurer configurer = new CustomScopeConfigurer();
      configurer.addScope("thread", new ClearableThreadScope());
      return configurer;
   }
}

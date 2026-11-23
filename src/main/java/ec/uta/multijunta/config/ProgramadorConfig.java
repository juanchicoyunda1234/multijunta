package ec.uta.multijunta.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "multijunta.programador.habilitado", havingValue = "true", matchIfMissing = true)
public class ProgramadorConfig {
}

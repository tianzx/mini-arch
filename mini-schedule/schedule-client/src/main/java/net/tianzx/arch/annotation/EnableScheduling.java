package net.tianzx.arch.annotation;

import net.tianzx.arch.config.SchedulingConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({SchedulingConfiguration.class})
//@ImportAutoConfiguration({SchedulingConfig.class, CronTaskRegister.class, DoJoinPoint.class})
@ComponentScan("net.tianzx.arch.*")
public @interface EnableScheduling {
}

package com.wizlit.path.logging;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
public class ReactorConfig {

    @PostConstruct
    public void init() {
        ContextRegistry registry = ContextRegistry.getInstance();

        registry.registerThreadLocalAccessor(
                RequestContextFilter.REQUEST_ID,
                () -> MDC.get(RequestContextFilter.REQUEST_ID),
                value -> MDC.put(RequestContextFilter.REQUEST_ID, value),
                () -> MDC.remove(RequestContextFilter.REQUEST_ID)
        );

        registry.registerThreadLocalAccessor(
                RequestContextFilter.USER_ID,
                () -> MDC.get(RequestContextFilter.USER_ID),
                value -> MDC.put(RequestContextFilter.USER_ID, value),
                () -> MDC.remove(RequestContextFilter.USER_ID)
        );

        Hooks.enableAutomaticContextPropagation();
    }
}

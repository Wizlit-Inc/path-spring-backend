package com.wizlit.path.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Aspect
@Component
@Slf4j
@Order(1)
public class ControllerLoggingAspect {

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logWebFlux(ProceedingJoinPoint jp) throws Throwable {
        String signature = jp.getSignature().toShortString();
        Object result = jp.proceed();
        if (result instanceof Mono) {
            return ((Mono<?>) result)
                    .doOnSubscribe(s -> log.info("▶▶▶▶▶▶▶▶▶▶ Enter {}", signature))
                    .doFinally(sig -> log.info("◀◀◀◀◀◀◀◀◀◀ Exit {}", signature));
        }
        if (result instanceof Flux) {
            return ((Flux<?>) result)
                    .doOnSubscribe(s -> log.info("▶▶▶▶▶▶▶▶▶▶ Enter {}", signature))
                    .doFinally(sig -> log.info("◀◀◀◀◀◀◀◀◀◀ Exit {}", signature));
        }
        return result;
    }
}
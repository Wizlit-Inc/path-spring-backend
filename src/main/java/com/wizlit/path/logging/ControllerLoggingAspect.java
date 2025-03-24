package com.wizlit.path.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
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

        if (result instanceof Mono<?> mono) {
            return Mono.deferContextual(ctx -> {
                String reqId = ctx.get(RequestContextFilter.REQUEST_ID);
                MDC.put(RequestContextFilter.REQUEST_ID, reqId);
                log.info("▶▶▶▶▶▶▶▶▶▶ Enter {}", signature);

                return mono
                        .doOnSuccess(k -> log.info("◀◀◀◀◀◀◀◀◀◀ Exit {}", signature))
                        .doFinally(s -> MDC.remove(RequestContextFilter.REQUEST_ID));
            });
        }
        if (result instanceof Flux<?> flux) {
            return Flux.deferContextual(ctx -> {
                String reqId = ctx.get(RequestContextFilter.REQUEST_ID);
                MDC.put(RequestContextFilter.REQUEST_ID, reqId);
                log.info("▶▶▶▶▶▶▶▶▶▶ Enter {}", signature);

                return flux
                        .doOnComplete(() -> log.info("◀◀◀◀◀◀◀◀◀◀ Exit {}", signature))
                        .doFinally(s -> MDC.remove(RequestContextFilter.REQUEST_ID));
            });
        }
        return result;
    }

}
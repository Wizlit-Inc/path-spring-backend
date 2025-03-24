package com.wizlit.path.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
@Order(2)
public class ServiceLoggingAspect {

    // 1️⃣ Matches any public Mono-returning method in your services
    @Pointcut("execution(public reactor.core.publisher.Mono com.wizlit.path.service.*.*(..))")
    public void monoServiceMethods() {}

    // 2️⃣ Matches any public Flux-returning method in your services
    @Pointcut("execution(public reactor.core.publisher.Flux com.wizlit.path.service.*.*(..))")
    public void fluxServiceMethods() {}

    // Combines both into a single “reactive” pointcut
    @Pointcut("monoServiceMethods() || fluxServiceMethods()")
    public void reactiveServiceMethods() {}

    @Around("reactiveServiceMethods()")
    public Object logReactiveService(ProceedingJoinPoint jp) throws Throwable {
        String className = jp.getSignature().toShortString();
//        String method = jp.getSignature().getName();
        Object[] args = jp.getArgs();
        Object result = jp.proceed();

        if (result instanceof Mono<?> mono) {
            return Mono.deferContextual(ctx -> {
                String reqId = ctx.get(RequestContextFilter.REQUEST_ID);
                return mono
                        .doOnSubscribe(s -> {
                            MDC.put(RequestContextFilter.REQUEST_ID, reqId);
                            log.info("▶▶ Mono ▶▶ {} <= {}", className, Arrays.toString(args));
                        })
                        .doOnSuccess(r -> log.info("◀◀ Mono ◀◀ {} => {}", className, r))
                        .doOnError(e -> log.error("‼ {} failed: {}", className, e.toString()))
                        .doFinally(_ -> MDC.remove(RequestContextFilter.REQUEST_ID));
            });
        }
        else if (result instanceof Flux<?> flux) {
            return Flux.deferContextual(ctx -> {
                    String reqId = ctx.get(RequestContextFilter.REQUEST_ID);
                    return flux
                            .doOnSubscribe(s -> {
                                MDC.put(RequestContextFilter.REQUEST_ID, reqId);
                                log.info("▶▶ Mono ▶▶ {} <= {}", className, Arrays.toString(args));
                            })
                            .doOnNext(r -> log.info("◀◀ Mono ◀◀ {} => {}", className, r))
                            .doOnError(e -> log.error("‼ {} failed: {}", className, e.toString()))
                            .doFinally(_ -> MDC.remove(RequestContextFilter.REQUEST_ID));
                });
        }

        return result;
    }

}
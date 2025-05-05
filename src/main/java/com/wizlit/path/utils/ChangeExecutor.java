package com.wizlit.path.utils;

import com.wizlit.path.model.LastChange;
import com.wizlit.path.model.ResponseWithChange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.Duration;
import java.util.function.Function;

public class ChangeExecutor {

    public static <Main, Check> Mono<ResponseWithChange<Main>> executeOnChange(
            Long lastSeenTs,
            Function<Instant, Mono<LastChange<Check>>> checkFunc,
            Function<Check, Mono<Main>> mainFunc
    ) {
        return executeOnChange(lastSeenTs, null, checkFunc, mainFunc);
    }

    public static <Main, Check> Mono<ResponseWithChange<Main>> executeOnChange(
            Long lastSeenTs,
            Long delayTs,
            Function<Instant, Mono<LastChange<Check>>> checkFunc,
            Function<Check, Mono<Main>> mainFunc
    ) {
        Instant currentInstant = Instant.now();

        // Treat null or non-positive as “always run”
        if (lastSeenTs == null || lastSeenTs <= 0) {
            return mainFunc.apply(null)
                .map(data -> new ResponseWithChange<>(
                        data,
                        currentInstant.toEpochMilli()
                ));
        }

        Instant inputInstant = Instant.ofEpochMilli(lastSeenTs);
        
        // If the API is called before the delay window has elapsed, skip execution
        if (delayTs != null && currentInstant.isBefore(inputInstant.plus(Duration.ofMillis(delayTs)))) {
            return Mono.just(new ResponseWithChange<>(
                    null,
                    inputInstant.toEpochMilli()
            ));
        }
        
        // Otherwise only run if there’s an actual update
        return checkFunc.apply(inputInstant)
                .flatMap(lastChange -> {
                    Instant newTs = lastChange.lastChangeTime();
                    System.out.println("newTs: " + newTs);
                    if (newTs.isAfter(inputInstant)) {
                        return mainFunc.apply(lastChange.data())
                                .map(data -> new ResponseWithChange<>(
                                        data,
                                        newTs.toEpochMilli()
                                ));
                    } else {
                        return Mono.just(new ResponseWithChange<>(
                                null,
                                inputInstant.toEpochMilli()
                        ));
                    }
                });
    }

}
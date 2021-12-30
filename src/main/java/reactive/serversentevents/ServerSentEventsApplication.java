package reactive.serversentevents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class ServerSentEventsApplication {
    //    functional reactive
    @Autowired
    GreetingService greetingService;

    @Bean
    RouterFunction<ServerResponse> routes1(GreetingService greetingService) {
        return route().GET("/functionalStyleGreeting/{name}", new HandlerFunction<ServerResponse>() {
            @Override
            public Mono<ServerResponse> handle(ServerRequest serverRequest) {
                Mono<GreetingResponse> greet = greetingService.greetOnce(new GreetingRequest(serverRequest.pathVariable("name")));
                return ok().body(greet, GreetingResponse.class);
            }
        }).build();
    }

    // concise functional reactive
    @Bean
    RouterFunction<ServerResponse> routes2(GreetingService greetingService) {
        return route()
                .GET("/conciseFunctionalStyleGreeting/{name}", hf -> ok().body(greetingService.greetOnce(new GreetingRequest(hf.pathVariable("name"))), GreetingResponse.class))
                .GET("/greetings/{name}", r -> ok().contentType(MediaType.TEXT_EVENT_STREAM).body(greetingService.greetMany(new GreetingRequest(r.pathVariable("name"))), GreetingResponse.class))
                .GET("/greetingWithHandler/{name}",this::greetingHandler)
                .build();

    }

    Mono<ServerResponse> greetingHandler(ServerRequest serverRequest) {
        return ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(greetingService.greetMany(new GreetingRequest(serverRequest.pathVariable("name"))), GreetingResponse.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(ServerSentEventsApplication.class, args);
    }

}

//
@RestController
class GreetingController {
    private final GreetingService greetingService;

    GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/greeting/{name}")
    Mono<GreetingResponse> greet(@PathVariable String name) {
        return this.greetingService.greetOnce(new GreetingRequest(name));
    }
}

@Service
class GreetingService {
    //Search for BlockHound
    Flux<GreetingResponse> greetMany(GreetingRequest request) {
        return Flux
                .fromStream(Stream.generate(() ->
                        greet(request.getName())))
                .delayElements(Duration.ofSeconds(1));
        //.subscribeOn(Schedulers.immediate());//explore
        //.subscribeOn(Schedulers.boundedElastic());
        //.subscribeOn(Schedulers.parallel());
    }

    Mono<GreetingResponse> greetOnce(GreetingRequest request) {
        return Mono.just(greet(request.getName()));
    }

    private GreetingResponse greet(String name) {
        return new GreetingResponse("Hello " + name + "@" + Instant.now());
    }


}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse {
    private String message;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest {
    private String name;
}
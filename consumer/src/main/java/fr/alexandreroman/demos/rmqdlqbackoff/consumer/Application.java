/*
 * Copyright (c) 2019 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.alexandreroman.demos.rmqdlqbackoff.consumer;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.ImmediateAcknowledgeAmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Component
class ServiceToggleStatus {
    private boolean enabled;

    boolean enabled() {
        return enabled;
    }

    boolean toggle() {
        enabled = !enabled;
        return enabled;
    }
}

@RestController
@RequiredArgsConstructor
class ServiceToggleController {
    private final ServiceToggleStatus serviceStatus;

    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    String status() {
        return "Service is " + (serviceStatus.enabled() ? "ENABLED" : "DISABLED");
    }

    @GetMapping(value = "/toggle", produces = MediaType.TEXT_PLAIN_VALUE)
    String toggle() {
        final var status = this.serviceStatus.toggle();
        return "Service is now " + (status ? "ENABLED" : "DISABLED");
    }
}

@Configuration
@RequiredArgsConstructor
@EnableBinding(Sink.class)
@Slf4j
class StockListener {
    private final ServiceToggleStatus serviceStatus;
    @Value("${app.stock.maxAttemptsBeforeEviction:6}")
    private Long maxAttempts;

    @StreamListener(Sink.INPUT)
    void onStockUpdate(StockUpdateRequest req,
                       @Header(name = "x-death", required = false) Map<?, ?> death) {
        if (!serviceStatus.enabled()) {
            if (death != null && maxAttempts.equals(death.get("count"))) {
                throw new ImmediateAcknowledgeAmqpException(
                        "Discarding stock update (too old): " + req.getStock() + "=" + req.getValue());
            }
            throw new IllegalStateException(
                    "Cannot process stock update: " + req.getStock() + "=" + req.getValue());
        }
        log.info("Received stock update: {}={}", req.getStock(), req.getValue());
    }
}

@Data
@RequiredArgsConstructor
class StockUpdateRequest {
    private final String stock;
    private final BigDecimal value;
    private final OffsetDateTime timeUpdated;
}

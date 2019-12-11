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

package fr.alexandreroman.demos.rmqdlqbackoff.producer;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.http.MediaType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Random;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@RestController
@RequiredArgsConstructor
@EnableBinding(Source.class)
@Slf4j
class IndexController {
    private static final String[] STOCK_NAMES = {
            "VMW", "AAPL", "GOOGL", "AMZN", "MSFT"
    };

    private final Source source;
    private final Random random = new Random();

    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    String index() {
        final var elemCount = 10;
        log.info("About to send {} stock updates", elemCount);

        for (int i = 0; i < elemCount; ++i) {
            final var stock = STOCK_NAMES[random.nextInt(STOCK_NAMES.length)];
            final var value = BigDecimal.valueOf(random.nextInt(1000));
            final var timeUpdated = OffsetDateTime.now();
            final var req = new StockUpdateRequest(stock, value, timeUpdated);
            log.info("Sending stock update: {}={}", stock, value);
            source.output().send(MessageBuilder.withPayload(req).build());
        }

        return String.format("Injected %d elements", elemCount);
    }
}

@Data
@RequiredArgsConstructor
class StockUpdateRequest {
    private final String stock;
    private final BigDecimal value;
    private final OffsetDateTime timeUpdated;
}

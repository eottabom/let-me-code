package com.eottabom.letmecode.example.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class RedisDnsCacheApplicationTests {

    @Test
    void inspectUnknownHostDoesNotThrow() {
        DnsCacheInspector inspector = new DnsCacheInspector();

        assertThatCode(() -> inspector.inspect("redis-host.invalid"))
                .doesNotThrowAnyException();
    }

    @Test
    void mainWithoutArgumentsDoesNotThrow() {
        assertThatCode(() -> RedisDnsCacheApplication.main(new String[0]))
                .doesNotThrowAnyException();
    }
}

package com.eottabom.letmecode.example.gateway;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 기존 역방향 프록시가 백엔드 응답에 붙이던 접두어를 재현한다. Istio Gateway 전환 후 이 prefix 가 사라지면(=백엔드 응답 그대로)
 * Gateway 가 직접 라우팅했다는 시각적 증거가 된다.
 *
 * NettyWriteResponseFilter 보다 먼저(더 바깥쪽에서) 실행되어야 응답 데코레이터가 실제 write 단계에 반영되므로
 * Ordered.HIGHEST_PRECEDENCE 를 사용한다.
 */
public class PrefixResponseBodyFilter implements GatewayFilter, Ordered {

	private final String prefix;

	public PrefixResponseBodyFilter(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpResponse originalResponse = exchange.getResponse();
		DataBufferFactory bufferFactory = originalResponse.bufferFactory();

		ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(originalResponse) {
			@Override
			public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
				Flux<DataBuffer> prefixed = Flux.from(body).collectList().map(dataBuffers -> {
					DataBuffer joined = bufferFactory.join(dataBuffers);
					byte[] content = new byte[joined.readableByteCount()];
					joined.read(content);
					DataBufferUtils.release(joined);

					String result = prefix + new String(content, StandardCharsets.UTF_8);
					byte[] resultBytes = result.getBytes(StandardCharsets.UTF_8);
					getHeaders().setContentLength(resultBytes.length);
					return bufferFactory.wrap(resultBytes);
				}).flux();
				return super.writeWith(prefixed);
			}
		};

		return chain.filter(exchange.mutate().response(decorated).build());
	}

}

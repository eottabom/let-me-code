package com.eottabom.letmecode.example.authz;

import com.google.rpc.Status;
import io.envoyproxy.envoy.config.core.v3.HeaderValue;
import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.AuthorizationGrpc;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import io.envoyproxy.envoy.service.auth.v3.DeniedHttpResponse;
import io.envoyproxy.envoy.service.auth.v3.OkHttpResponse;
import io.envoyproxy.envoy.type.v3.HttpStatus;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Envoy ext_authz v3 gRPC 인터페이스 구현체. Istio Gateway(Envoy) 가 요청마다 {@link #check} 를 호출한다.
 */
@Service
public class ExtAuthzGrpcService extends AuthorizationGrpc.AuthorizationImplBase {

	private static final String DEMO_USER_HEADER = "x-demo-user";
	private static final String AUTHZ_USER_HEADER = "x-authz-user";
	private static final String AUTHZ_RESULT_HEADER = "x-authz-result";

	@Override
	public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
		String demoUser = request.getAttributes()
			.getRequest()
			.getHttp()
			.getHeadersOrDefault(DEMO_USER_HEADER, "");

		if (isAllowed(demoUser)) {
			responseObserver.onNext(allow(demoUser));
		} else {
			responseObserver.onNext(deny());
		}
		responseObserver.onCompleted();
	}

	private boolean isAllowed(String demoUser) {
		String trimmed = demoUser.trim();
		return !trimmed.isEmpty() && !trimmed.toLowerCase(Locale.ROOT).equals("deny");
	}

	private CheckResponse allow(String demoUser) {
		return CheckResponse.newBuilder()
			.setStatus(Status.newBuilder().setCode(0).build())
			.setOkResponse(OkHttpResponse.newBuilder()
				.addHeaders(headerOption(AUTHZ_USER_HEADER, demoUser))
				.addHeaders(headerOption(AUTHZ_RESULT_HEADER, "allow"))
				.build())
			.build();
	}

	private CheckResponse deny() {
		return CheckResponse.newBuilder()
			.setStatus(Status.newBuilder().setCode(7).setMessage("missing " + DEMO_USER_HEADER).build())
			.setDeniedResponse(DeniedHttpResponse.newBuilder()
				.setStatus(HttpStatus.newBuilder().setCodeValue(403).build())
				.addHeaders(headerOption(AUTHZ_RESULT_HEADER, "denied"))
				.setBody("DENY by ext_authz: add header " + DEMO_USER_HEADER + "\n")
				.build())
			.build();
	}

	private HeaderValueOption headerOption(String key, String value) {
		return HeaderValueOption.newBuilder()
			.setHeader(HeaderValue.newBuilder().setKey(key).setValue(value).build())
			.build();
	}
}

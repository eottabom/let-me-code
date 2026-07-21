package com.eottabom.letmecode.example.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.eottabom.letmecode.example.PersonProto;
import com.eottabom.letmecode.example.PersonServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PersonClient {

	private static final Logger logger = LoggerFactory.getLogger(PersonClient.class);

	private PersonClient() {

	}

	public static void main(String[] args) {
		// gRPC 채널 생성
		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();

		// gRPC 서비스를 호출할 stub 생성
		PersonServiceGrpc.PersonServiceBlockingStub stub = PersonServiceGrpc.newBlockingStub(channel);

		PersonProto.PersonRequest request = PersonProto.PersonRequest.newBuilder().setName("eottabom").build();

		// 서버에 RPC 호출을 보내고 응답을 받음
		PersonProto.PersonResponse response = stub.getPerson(request);

		logger.info("Server response : \n {}", response.toString());

		// 채널 종료
		channel.shutdown();
	}

}

package com.eottabom.letmecode.example.grpc;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import com.eottabom.letmecode.example.PersonProto;
import com.eottabom.letmecode.example.PersonServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PersonServer {

	private static final Logger logger = LoggerFactory.getLogger(PersonServer.class);

	private PersonServer() {

	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Server server = ServerBuilder.forPort(50051).addService(new PersonServiceImpl()).build();

		logger.info("gRPC server start");
		server.start();

		server.awaitTermination();
	}

	static class PersonServiceImpl extends PersonServiceGrpc.PersonServiceImplBase {

		@Override
		public void getPerson(PersonProto.PersonRequest request,
				StreamObserver<PersonProto.PersonResponse> responseObserver) {

			String name = request.getName();
			PersonProto.PersonResponse response;

			PersonProto.Address address = PersonProto.Address.newBuilder().setCity("Seoul").setZipCode("1234").build();

			PersonProto.Person person = PersonProto.Person.newBuilder()
				.setName(name)
				.setAge(35)
				.setPhoneNumber("010-1234-1234")
				.setAddress(address)
				.build();

			response = PersonProto.PersonResponse.newBuilder().setPerson(person).build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}

	}

}

package com.eottabom.letmecode.example.benchmark.serialization;

import java.util.concurrent.TimeUnit;

import com.eottabom.letmecode.example.benchmark.PersonProto;
import com.eottabom.letmecode.example.benchmark.serialization.domain.Address;
import com.eottabom.letmecode.example.benchmark.serialization.domain.Person;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 3)
@Measurement(iterations = 10, time = 3)
public class SerializationBenchmark {

	private static final OkHttpClient client = new OkHttpClient();

	private static final ObjectMapper objectMapper = new ObjectMapper()
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	private static final MediaType PROTOBUF = MediaType.get("application/x-protobuf");

	private Person jsonPerson;

	private PersonProto.Person protoPerson;

	@Setup
	public void setup() {

		this.jsonPerson = new Person();
		this.jsonPerson.setName("eottabom");
		this.jsonPerson.setAge(35);
		this.jsonPerson.setPhoneNumber("010-1234-1234");

		Address address = new Address();
		address.setCity("Seoul");
		address.setZipCode("1234");
		this.jsonPerson.setAddress(address);

		this.protoPerson = PersonProto.Person.newBuilder()
			.setName(this.jsonPerson.getName())
			.setAge(this.jsonPerson.getAge())
			.setPhoneNumber(this.jsonPerson.getPhoneNumber())
			.setAddress(PersonProto.Address.newBuilder()
				.setCity(address.getCity())
				.setZipCode(address.getZipCode())
				.build())
			.build();
	}

	// JSON 직렬화/역직렬화 벤치마크
	@Benchmark
	public void jsonBenchmark() throws Exception {
		// 직렬화
		String jsonData = objectMapper.writeValueAsString(this.jsonPerson);

		// HTTP 요청
		RequestBody body = RequestBody.create(jsonData, JSON);
		Request request = new Request.Builder().url("http://localhost:8080/api/person/json").post(body).build();

		Response response = client.newCall(request).execute();
		String responseData = response.body().string();

		// 역직렬화
		Person responsePerson = objectMapper.readValue(responseData, Person.class);
	}

	// Protocol Buffers 직렬화/역직렬화 벤치마크
	@Benchmark
	public void protobufBenchmark() throws Exception {
		// 직렬화
		byte[] protoData = this.protoPerson.toByteArray();

		// HTTP 요청
		RequestBody body = RequestBody.create(protoData, PROTOBUF);
		Request request = new Request.Builder().url("http://localhost:8080/api/person/protobuf").post(body).build();

		Response response = client.newCall(request).execute();
		byte[] responseData = response.body().bytes();

		// 역직렬화
		PersonProto.Person responsePerson = PersonProto.Person.parseFrom(responseData);
	}

}

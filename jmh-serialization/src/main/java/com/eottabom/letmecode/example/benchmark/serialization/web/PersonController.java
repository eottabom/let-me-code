package com.eottabom.letmecode.example.benchmark.serialization.web;

import com.eottabom.letmecode.example.benchmark.PersonProto;
import com.eottabom.letmecode.example.benchmark.serialization.domain.Person;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PersonController {

	private final ObjectMapper objectMapper = new ObjectMapper();

	// JSON 데이터 처리
	@PostMapping(value = "/person/json", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public Person handleJson(@RequestBody Person person) {
		// 간단하게 역직렬화된 데이터를 그대로 반환 (여기서는 처리 과정 생략)
		return person;
	}

	// Protocol Buffers 데이터 처리
	@PostMapping(value = "/person/protobuf", consumes = "application/x-protobuf", produces = "application/x-protobuf")
	public byte[] handleProtobuf(@RequestBody byte[] data) throws Exception {
		// Protocol Buffers로 역직렬화
		PersonProto.Person person = PersonProto.Person.parseFrom(data);

		// 처리 후 다시 직렬화하여 반환
		return person.toByteArray();
	}

}

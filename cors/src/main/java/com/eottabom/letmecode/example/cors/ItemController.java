package com.eottabom.letmecode.example.cors;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{policy}/items")
public class ItemController {

	private final RequestRecorder requestRecorder;

	public ItemController(RequestRecorder requestRecorder) {
		this.requestRecorder = requestRecorder;
	}

	@GetMapping
	public ResponseEntity<List<String>> list(@PathVariable String policy, HttpServletRequest request) {
		this.requestRecorder.recordHandled(request);
		return ResponseEntity.ok().header("X-Total-Count", "2").body(List.of("a", "b"));
	}

	@PostMapping
	public String create(@PathVariable String policy, @RequestBody(required = false) String body,
			HttpServletRequest request) {
		this.requestRecorder.recordHandled(request);
		return "created";
	}

	@PutMapping
	public String update(@PathVariable String policy, HttpServletRequest request) {
		this.requestRecorder.recordHandled(request);
		return "updated";
	}

}

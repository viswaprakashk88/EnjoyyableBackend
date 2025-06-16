package com.example.Example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
	
	@GetMapping("firstCall")
	public String firstApiCall () {
		return "This is an example";
	}
}

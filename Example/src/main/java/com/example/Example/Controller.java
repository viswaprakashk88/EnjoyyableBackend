package com.example.Example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class Controller {
	
	@Autowired
	private Environment env;
	
	public String scopes = "user-read-private user-read-email streaming user-modify-playback-state user-read-playback-state";
	
	@PostMapping("/firstCall")
	public List<Map<String, String>> firstCall (@RequestBody Map<String,String> request) {
		
		List<Map<String,String>> list = new ArrayList<>();
		
		Map<String, String> json = new HashMap<>();
		json.put("message", "First API Call");
		json.put("description", "This is the first API Call's response description");
		list.add(json);
		
		System.out.println("body code value is: " + request.get("code"));
		
		
		return list;
	}
	
	@PostMapping("/getEnv")
	public ResponseEntity<Map<String,Object>> getEnv(@RequestBody Map<String, String> body ) {
		Map<String,Object> response = new HashMap<String,Object>();
		if(body.get("name")==null) {
			response.put("ok",false);
			response.put("error_message", "name is not present");
			return ResponseEntity.badRequest().body(response);
		}
		response.put("ok",true);
		response.put("encrypted name", "qwerty" + env.getProperty("CLIENT_ID") + ", " + body.get("code"));
		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/firstCall")
	public ResponseEntity<Map<String,Object>> fallback() {
		Map<String, Object> response = new HashMap<String,Object>();
		response.put("error_message", "Hey There! Please use POST Method to get the response");
		response.put("ok", false);
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/login")
	public RedirectView login() {
		RedirectView redirectView = new RedirectView();
		redirectView.setUrl("https://accounts.spotify.com/authorize?response_type=code&redirect_uri=http://localhost:3000/&client_id=" + env.getProperty("CLIENT_ID") + "&scope=" + scopes);
		return redirectView;
	}
	
	@GetMapping("/login")
	public RedirectView loginGet() {
		RedirectView redirectView = new RedirectView();
		redirectView.setUrl("https://accounts.spotify.com/authorize?response_type=code&redirect_uri=http://localhost:3000/&client_id=" + env.getProperty("CLIENT_ID") + "&scope=" + scopes);
		return redirectView;
	}
	
	@PostMapping("/accessToken")
	public ResponseEntity<Map<String,Object>> accessToken (@RequestBody Map<String, String> body) {
		
		String code = body.get("code");
		System.out.println("Here is the code : " + code);
		String URL = "https://accounts.spotify.com/api/token";
		
		String authString = env.getProperty("CLIENT_ID") + ":" + env.getProperty("CLIENT_SECRET");
		String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
		
		MultiValueMap<String, String> tempBody = new LinkedMultiValueMap<>();
		tempBody.add("grant_type", "client_credentials");
		tempBody.add("code", code);
//		tempBody.add("redirect_uri", "http://localhost:3000");
		
		Map<String, Object> finalResponse = new HashMap<String, Object>();
		
//		if (true) {
//			finalResponse.put("tempBody", tempBody);
//			finalResponse.put("headers", headers);
//			
//			ResponseEntity.ok(finalResponse);
//		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.set("Authorization", "Basic " + encodedAuthString);
		
		
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(tempBody, headers);
		RestTemplate restTemplate = new RestTemplate();
		
		ResponseEntity<Map> response = restTemplate.postForEntity(URL, request, Map.class);
		
		
		
		if (response.getStatusCode() != HttpStatus.OK) {
			finalResponse.put("message", response.getStatusCode()+"");
			finalResponse.put("message", response.getBody());
//			finalResponse.put("accessToken", response.);
			ResponseEntity.badRequest().body(finalResponse);
		}
		return ResponseEntity.ok(response.getBody());
	}

}	

package com.example.Example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class Controller {
	
	@Autowired
	private Environment env;
	
	@Autowired
	private AmazonDynamoDB dynamodDb;
	
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
		String URL = "https://accounts.spotify.com/api/token";
		
		System.out.println("code: " + code);
		
		//Headers
		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		String auth = env.getProperty("CLIENT_ID") + ":" + env.getProperty("CLIENT_SECRET");
		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
		
		headers.set("Authorization", "Bearer " + auth);
		
		MultiValueMap<String, String> tempBody = new LinkedMultiValueMap<>();
		tempBody.add("code", code);
		tempBody.add("redirect_uri", "http://localhost:3000/");
		tempBody.add("grant_type", "authorization_code");
		
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(tempBody, headers);
		
		Map<String, Object> response = new HashMap<String, Object>();
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<Map> res = restTemplate.postForEntity(URL, request, Map.class);
		
		return ResponseEntity.ok(res.getBody());
	}
	
	
	@PostMapping("/getUsers")
	public ResponseEntity<Map<String,Object>> getUsers (@RequestBody Map<String, String> body) {
		
		try {
			
			Map<String,Object> response = new HashMap<String,Object>();
			
			AttributeValue userValue = new AttributeValue(body.get("user"));
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":user", userValue);
			
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#user", "user");
			attributeNames.put("#name", "name");
			attributeNames.put("#mobile", "mobile");
			attributeNames.put("#email", "email");
			
			
			
			QueryRequest queryRequest = new QueryRequest()
					.withTableName("enjoyyable_users")
					.withKeyConditionExpression("#user =:user")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues)
					.withProjectionExpression("#name, #email, #user, #mobile");
			
			QueryResult queryResult = dynamodDb.query(queryRequest);
			
			
			response.put("ok", "true");
			
			List<Map<String,String>> users = new ArrayList<>();
			
			for (Map<String,AttributeValue> i: queryResult.getItems()) {
				Map<String, String> temp = new HashMap<String,String>();
				
				temp.put("name", i.get("name").getS());
				temp.put("user", i.get("user").getS());
				
				users.add(temp);
			}
			
			response.put("users",users);
			
			return ResponseEntity.ok(response);
			
		}
		catch (Exception e) {
			
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		
		
	}
	 
	
	
	@PostMapping("/checkLoginCredentials")
	public ResponseEntity<Map<String,Object>> checkLoginCredentials (@RequestBody Map<String,String> body) {
		
		try {
			Map<String,Object> response = new HashMap<String,Object>();
			System.out.println("200");

			AttributeValue user = new AttributeValue(body.get("username"));
			
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#user","user");
			attributeNames.put("#password","password");
			
			System.out.println("207");
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":user", user);
			
			QueryRequest queryRequest = new QueryRequest()
					.withTableName("enjoyyable_users")
					.withKeyConditionExpression("#user =:user")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues)
					.withProjectionExpression("#password");
			System.out.println("217");
			
			QueryResult queryResult = dynamodDb.query(queryRequest);
			
			response.put("count", queryResult.getCount());
			
			response.put("ok", "true");
			System.out.println(queryResult.getCount());
			
			System.out.println("227");
			
			if (queryResult.getCount() < 1) {
				response.put("loginStatus", "failure");
				return ResponseEntity.ok(response);
			}
			
			for (Map<String,AttributeValue> i: queryResult.getItems()) {
				if (i.get("password").getS().equals(body.get("password").toString())) {
					response.put("loginStatus", "success");
				} else {
					response.put("loginStatus", "failure");
				}
				break;
			}
			
			return ResponseEntity.ok(response);
		
		} catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("loginStatus", "failure");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	//Get Connection Requests
	@PostMapping("/getRequests")
	public ResponseEntity<Map<String,Object>> getRequests (@RequestBody Map<String,String> body) {
		try {
			Map<String,Object> response = new HashMap<String,Object>();
			
			AttributeValue user = new AttributeValue(body.get("username"));
			
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#fromUsername","fromUsername");
			attributeNames.put("#toUsername","toUsername");
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":fromUsername", user);
			
			ScanRequest scanRequest = new ScanRequest()
					.withTableName("connection_requests")
					.withFilterExpression("#fromUsername =:fromUsername OR #toUsername=:fromUsername")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues);
			
			ScanResult scanResult = dynamodDb.scan(scanRequest);
			
			response.put("count", scanResult.getCount());
			response.put("ok", "true");
			
			ArrayList<Object> requests = new ArrayList<Object>();
			
			for (Map<String,AttributeValue> i: scanResult.getItems()) {
				Map<String,String> map = new HashMap<String,String>();
				for (Map.Entry<String,AttributeValue> j : i.entrySet()) {
					map.put(j.getKey().toString(),j.getValue().getS());
				}
				requests.add(map);
				
			}
			
			response.put("requests", requests);
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	
	//API to update the name
	@PostMapping ("/updateName")
	public ResponseEntity<Map<String,Object>> updateName (@RequestBody Map<String,String> body) {
		
		try {
			
			Map<String,AttributeValue> key = new HashMap<String,AttributeValue>();
			key.put("user",new AttributeValue(body.get("username")));
			
			Map<String,Object> response = new HashMap<String,Object>();
			
			AttributeValue name = new AttributeValue(body.get("name"));
			
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#name","name");
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":name", name);
			
			UpdateItemRequest updateItem = new UpdateItemRequest()
					.withTableName("enjoyyable_users")
					.withKey(key)
					.withUpdateExpression("SET #name = :name")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues);
			
			UpdateItemResult result = dynamodDb.updateItem(updateItem);
			
//			AttributeValue groupIdname = new AttributeValue(body.get("groupIdname"));
//			
//			attributeNames = new HashMap<String,String>();
//			attributeNames.put("#fromUsername","fromUsername");
//			attributeNames.put("#toUsername","toUsername");
//			
//			attributeValues = new HashMap<String,AttributeValue>();
//			attributeValues.put(":username", new AttributeValue(body.get("username")));
//			
//			QueryRequest queryRequest = new QueryRequest()
//					.withTableName("connection_requests")
//					.withKeyConditionExpression("#toUsername=:username OR #fromUsername=:username")
//					.withExpressionAttributeNames(attributeNames)
//					.withExpressionAttributeValues(attributeValues);
//			
//			QueryResult queryResult = dynamodDb.query(queryRequest);
//			
//			for (Map<String, AttributeValue>i: queryResult.getItems()) {
//				
//			}
			
			
			response.put("result", result.getSdkHttpMetadata().getHttpStatusCode() + "");
			response.put("ok","true");
			
			return ResponseEntity.ok(response);			
			
		} catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
		
	}
	
	
	//Route to get the groups that the user is part of
	@PostMapping("/getGroups")
	public ResponseEntity<Map<String,Object>> getGroups (@RequestBody Map<String,String> body) {
		try {
			Map<String,Object> response = new HashMap<String,Object>();
			
			AttributeValue user = new AttributeValue(body.get("username"));
			
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#groupMembers","groupMembers");
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":user", user);
			
			ScanRequest scanRequest = new ScanRequest()
					.withTableName("groups")
					.withFilterExpression("contains(#groupMembers, :user)")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues);
			
			ScanResult scanResult = dynamodDb.scan(scanRequest);
			
			response.put("count", scanResult.getCount());
			response.put("ok", "true");
			
			ArrayList<Object> groups = new ArrayList<Object>();
			
			for (Map<String,AttributeValue> i: scanResult.getItems()) {
				Map<String,String> map = new HashMap<String,String>();
				for (Map.Entry<String,AttributeValue> j : i.entrySet()) {
					map.put(j.getKey().toString(),j.getValue().getS());
				}
				groups.add(map);
				
			}
			
			response.put("items", groups);
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	//Route to check whether the given groupId already exists or not.
	@PostMapping("/checkGroupId")
	public ResponseEntity<Map<String,Object>> checkGroupId (@RequestBody Map<String,String> body) {
		try {
			Map<String,Object> response = new HashMap<String,Object>();
			
			AttributeValue groupIdname = new AttributeValue(body.get("groupIdname"));
			
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#groupid","groupid");
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":groupIdname", groupIdname);
			
			QueryRequest queryRequest = new QueryRequest()
					.withTableName("groups")
					.withKeyConditionExpression("#groupid = :groupIdname")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues);
			
			QueryResult queryResult = dynamodDb.query(queryRequest);
			
			response.put("exists", queryResult.getCount() > 0 ? "true" : "false");
			response.put("ok", "true");
			
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	
	//Route to create a group name
	@PostMapping ("createGroup")
	public ResponseEntity<Map<String,Object>> createGroup (@RequestBody Map<String,String> body) {
		try {
			Map<String,Object> response = new HashMap<String,Object>();
			
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#groupid","groupid");
			
			Map<String,AttributeValue> item = new HashMap<String,AttributeValue>();
			item.put("groupid", new AttributeValue(body.get("groupIdname")));
			item.put("groupName", new AttributeValue(body.get("groupName")));
			item.put("groupMembers", new AttributeValue(body.get("groupMembers")));
			item.put("groupAdmin", new AttributeValue(body.get("groupAdmin")));
			item.put("dateTime", new AttributeValue(body.get("dateTime")));
			
			PutItemRequest putItemRequest = new PutItemRequest()
					.withTableName("groups")
					.withItem(item);
			
			PutItemResult putItemResult = dynamodDb.putItem(putItemRequest);
			
			
			response.put("created", putItemResult.getSdkHttpMetadata().getHttpStatusCode() == 200 ? "true" : "false");
			response.put("ok", "true");
			
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	@PostMapping ("/getUserInfo")
	public ResponseEntity<Map<String,Object>> getUserInfo (@RequestBody Map<String,String> body) {
		try {
			Map<String,Object> response = new HashMap<String,Object>();
			
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#user","user");
			attributeNames.put("#name","name");
			attributeNames.put("#mobile","mobile");
			attributeNames.put("#email","email");
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			
			attributeValues.put(":user", new AttributeValue(body.get("username")));
			
			QueryRequest queryRequest = new QueryRequest()
					.withTableName("enjoyyable_users")
					.withKeyConditionExpression("#user =:user")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues)
					.withProjectionExpression("#name, #email, #user, #mobile");
			
			QueryResult queryResult = dynamodDb.query(queryRequest);
			
			ArrayList<Map<String,String>> items = new ArrayList<Map<String,String>>();
			
			for (Map<String,AttributeValue> i: queryResult.getItems()) {
				Map<String,String> temp = new HashMap<String,String>();
				if (i.containsKey("name")) {
					temp.put("name",i.get("name").getS());
				}
				if (i.containsKey("user")) {
					temp.put("username",i.get("user").getS());
				}
				if (i.containsKey("email")) {
					temp.put("email",i.get("email").getS());
				}
				if (i.containsKey("mobile")) {
					temp.put("mobile",i.get("mobile").getS());
				}
				
				items.add(temp);
			}
			
			response.put("items", items);
			response.put("ok", "true");
			
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	
	@PostMapping ("searchUser")
	public ResponseEntity<Map<String,Object>> searchUser (@RequestBody Map<String,String> body) {
		try {
			
			Map<String,String> attributeNames = new HashMap<String,String> ();
			attributeNames.put("#username", "user");
			attributeNames.put("#name", "name");
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":hint", new AttributeValue(body.get("userHint")));
			
			ScanRequest scanRequest = new ScanRequest()
					.withTableName("enjoyyable_users")
					.withFilterExpression("contains(#username, :hint) OR contains(#name,:hint)")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues)
					.withProjectionExpression("#name,#username");
		
			ScanResult queryResult = dynamodDb.scan(scanRequest);
			
			ArrayList<Object> users = new ArrayList<Object>();
			
			for (Map<String,AttributeValue> i: queryResult.getItems()) {
				Map<String,String> temp = new HashMap<String,String>();
				for (Map.Entry<String,AttributeValue> j : i.entrySet()) {
					temp.put(j.getKey().toString(),j.getValue().getS());
//					System.out.println(j.getValue().getS());
				}
				users.add(temp);
			}
			
			Map<String,Object> response = new HashMap<String,Object>();
			response.put("items", users);
			response.put("ok", "true");
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	
	@PostMapping("/signUp")
	public ResponseEntity<Map<String,Object>> signUp (@RequestBody Map<String, String> body) {
		
		try {
			
			Map<String,Object> response = new HashMap<String,Object>();
			
			String username = body.get("username");
			String password = body.get("password");
			String email = body.get("email");
			String mobile= body.get("mobile");
			String name = body.get("name");
			
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#user", "user");
			attributeNames.put("#mobile", "mobile");
			attributeNames.put("#email", "email");
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":user", new AttributeValue(username));
			attributeValues.put(":email", new AttributeValue(email));
			attributeValues.put(":mobile", new AttributeValue(mobile));
			
			
			
			ScanRequest scanRequest = new ScanRequest()
					.withTableName("enjoyyable_users")
					.withFilterExpression("#user =:user OR #email = :email OR #mobile = :mobile")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues);
			
			ScanResult scanResult = dynamodDb.scan(scanRequest);
			
			response.put("ok", "true");
			if (scanResult.getCount() > 0) {
				response.put("isItemInserted", "false");
				response.put("isItemExists", "true");
				
				return ResponseEntity.ok(response);
			}
			
			Map<String,AttributeValue> item = new HashMap<String,AttributeValue>();
			item.put("user", new AttributeValue(username));
			item.put("email", new AttributeValue(email));
			item.put("password", new AttributeValue(password));
			item.put("name", new AttributeValue(name));
			item.put("mobile", new AttributeValue(mobile));
			
			PutItemRequest putItemRequest = new PutItemRequest()
					.withTableName("enjoyyable_users")
					.withItem(item);
			
			PutItemResult putItemResult = dynamodDb.putItem(putItemRequest);
						
			response.put("isItemInserted", putItemResult.getSdkHttpMetadata().getHttpStatusCode() == 200 ? "true" : "false");
			response.put("isItemExists", "false");
			
			
			return ResponseEntity.ok(response);
			
		}
		catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	@PostMapping ("/sendConnectionRequest")
	public ResponseEntity<Map<String,Object>> sendConnectionRequest (@RequestBody Map<String,String> body) {
		try {
			Map<String,Object> response = new HashMap<String,Object>();
			
			String sendFromUsername = body.get("sendFromUsername");
			String sendTime = body.get("sendTime");
			String sendToUsername = body.get("sendToUsername");
			String sendName = body.get("sendName");
			String sendMyName = body.get("sendMyName");
			
			Map<String,AttributeValue> item = new HashMap<String,AttributeValue>();
			item.put("time", new AttributeValue(sendTime));
			item.put("fromUsername", new AttributeValue(sendFromUsername));
			item.put("friendshipStatus", new AttributeValue("requested"));
			item.put("fromName", new AttributeValue(sendMyName));
			item.put("toName", new AttributeValue(sendName));
			item.put("toUsername", new AttributeValue(sendToUsername));
			PutItemRequest putItemRequest = new PutItemRequest()
					.withTableName("connection_requests")
					.withItem(item);
			
			PutItemResult putItemResult = dynamodDb.putItem(putItemRequest);
			
			
			response.put("requestSent", putItemResult.getSdkHttpMetadata().getHttpStatusCode() == 200 ? "true" : "false");
			response.put("ok", "true");
			
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("requestSent", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	@PostMapping ("/acceptRequest")
	public ResponseEntity<Map<String,Object>> acceptRequest (@RequestBody Map<String,String> body) {
		try {
			
			Map<String,AttributeValue> key = new HashMap<String,AttributeValue>();
			key.put("time", new AttributeValue(body.get("time")));
			
			Map<String,Object> response = new HashMap<String,Object>();
			
			
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#friendshipStatus","friendshipStatus");
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":friended", new AttributeValue("friends"));
			
			
			UpdateItemRequest updateItem = new UpdateItemRequest()
					.withTableName("connection_requests")
					.withKey(key)
					.withUpdateExpression("SET #friendshipStatus= :friended")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues);
			
			UpdateItemResult result = dynamodDb.updateItem(updateItem);
			
			response.put("result", result.getSdkHttpMetadata().getHttpStatusCode() + "");
			response.put("ok","true");
			
			return ResponseEntity.ok(response);			
			
		} catch (Exception e) {
			Map<String,Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
		
	}
	
	@PostMapping ("/getAllRequests")
	public ResponseEntity<Map<String,Object>> getAllRequests (@RequestBody Map<String,String> body) {
		try {
			
			String username = body.get("username");
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#toUsername", "toUsername");
			attributeNames.put("#friendshipStatus", "friendshipStatus");
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":friended", new AttributeValue("requested"));
			attributeValues.put(":toUsername", new AttributeValue(body.get("username")));
			
			ScanRequest scanRequest = new ScanRequest()
					.withTableName("connection_requests")
					.withFilterExpression("#toUsername=:toUsername AND #friendshipStatus = :friended")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues);
			
			ScanResult scanResult = dynamodDb.scan(scanRequest);
			
			
			ArrayList<Object> arrayList = new ArrayList<Object>();
			for (Map<String,AttributeValue> i : scanResult.getItems()) {
				Map<String,String> temp = new HashMap<>();
				for (Map.Entry<String,AttributeValue> j:i.entrySet()) {
					temp.put(j.getKey(), j.getValue().getS());
				}
				arrayList.add(temp);
			}
			
			Map<String,Object> response = new HashMap<String,Object>(); 
			
			response.put("items", arrayList);
			response.put("ok", "true");
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String, Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	@PostMapping ("/getFriends")
	public ResponseEntity<Map<String,Object>> getFriends (@RequestBody Map<String,String> body) {
		try {
			
			String username = body.get("username");
			Map<String,String> attributeNames = new HashMap<String,String>();
			attributeNames.put("#username", "username");
			attributeNames.put("#friendshipStatus", "friendshipStatus");
			
			Map<String,AttributeValue> attributeValues = new HashMap<String,AttributeValue>();
			attributeValues.put(":friended", new AttributeValue("friends"));
			attributeValues.put(":username", new AttributeValue(body.get("username")));
			
			ScanRequest scanRequest = new ScanRequest()
					.withTableName("connection_requests")
					.withFilterExpression("contains(#username,:username) AND #friendshipStatus = :friended")
					.withExpressionAttributeNames(attributeNames)
					.withExpressionAttributeValues(attributeValues);
			
			ScanResult scanResult = dynamodDb.scan(scanRequest);
			
			
			ArrayList<Object> arrayList = new ArrayList<Object>();
			for (Map<String,AttributeValue> i : scanResult.getItems()) {
				if (i.get("username").toString().endsWith(username)) {
					arrayList.add(i);
				}
			}
			
			Map<String,Object> response = new HashMap<String,Object>(); 
			
			response.put("items", arrayList);
			response.put("ok", "true");
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String, Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
	@PostMapping ("/getNames")
	public ResponseEntity<Map<String,Object>> getNames (@RequestBody Map<String,List<String>> body) {
		try {
			
			List<String> usernames = (List<String>)body.get("usernames");
			
			
			String tableName = "enjoyyable_users";
			
			List<Map<String,AttributeValue>> keys = new ArrayList<>();
			for (String i: usernames) {
				Map<String,AttributeValue> temp = new HashMap<String,AttributeValue>();
				temp.put("user", new AttributeValue(i));
				keys.add(temp);
			}
			KeysAndAttributes keysAndAttributes = new KeysAndAttributes().withKeys(keys);
			
			Map<String, KeysAndAttributes> request = new HashMap<>();
			request.put(tableName, keysAndAttributes);
			
			BatchGetItemRequest batchGetItem = new BatchGetItemRequest(request);
			
			BatchGetItemResult batchGetItemResult = dynamodDb.batchGetItem(batchGetItem);
			
			Map<String,Object> response = new HashMap<String,Object>(); 
			
			List<Map<String,String>> names = new ArrayList<>();
			
			for (Map<String,AttributeValue> i: batchGetItemResult.getResponses().get("enjoyyable_users")) {
				Map<String,String> temp = new HashMap<String,String>();
				temp.put("user",i.get("user").getS());
				temp.put("name",i.get("name").getS());
				names.add(temp);
			}
			
			response.put("items", names);
			response.put("ok", "true");
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			Map<String, Object> response = new HashMap<String,Object>();
			
			response.put("ok", "false");
			response.put("error_message", e.getMessage());
			
			return ResponseEntity.badRequest().body(response);
		}
	}
	
}	

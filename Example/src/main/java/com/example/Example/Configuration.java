package com.example.Example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

@org.springframework.context.annotation.Configuration
public class Configuration {
	
	@Autowired
	public Environment env;

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(
                env.getProperty("AWS_ACCESS_KEY_ID"),
                env.getProperty("AWS_SECRET_ACCESS_KEY")
        );

        return AmazonDynamoDBClientBuilder.standard()
                .withRegion("ap-south-2") // Replace with your region
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }
}
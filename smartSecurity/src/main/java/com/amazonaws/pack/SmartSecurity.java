package com.amazonaws.pack;

import java.util.List;
import java.util.Scanner;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.BatchGetItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableKeysAndAttributes;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class SmartSecurity {

	static AmazonDynamoDB dynamoDB;
	
	static double SoS = 171.5; // ultrasonic sensor constant
	
	public static void main(String[] args) throws Exception {
		System.out.println("------------------------------------------------");
		System.out.println("Connecting...");
		System.out.println("------------------------------------------------");

        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider("SJSU-AWS");
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (C:\\Users\\<username>\\.aws\\credentials), and is in valid format.",
                    e);
        }
        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion("us-west-1")
            .build();
        
        AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion("us-west-1")
                .build();
		
        System.out.println("------------------------------------------------");
        System.out.println("AWS Credentials Confirmed!");
        System.out.println("------------------------------------------------");
        
		DynamoDB DDB = new DynamoDB(dynamoDB);
		String smartLocksTableName = "smartLocks";
		Table smartLocksTable = DDB.getTable(smartLocksTableName);
		String smartUltraSensorsTableName = "smartUltraSensors";
		Table smartUltraSensorsTable = DDB.getTable(smartUltraSensorsTableName);
		        
		String FROM = "bredoloza@gmail.com";
		String TO = "brianalbert.redoloza@sjsu.edu";
	    Destination destination = new Destination().withToAddresses(new String[]{TO});
	    String SUBJECT = "";
	    String BODY = "";
	    Content subject;
        Content textBody;
        Body body;
        Message message;
        SendEmailRequest request;
        
        System.out.println("Welcome to the Smart Security IoT Dashboard!\n"
        		+ "Press a number key to navigate menus.\n");
        
        Scanner input = new Scanner(System.in);
        int operation = -1;
        while (operation != 0) {
			System.out.print("MAIN MENU:\n"
					+ "[1] Check Status of Smart Devices\n"
					+ "[2] Reset Device\n"
					+ "[0] Shut Down\n");
			System.out.println();
			operation = input.nextInt();
			switch (operation) { 
				case 1:
		        	TableKeysAndAttributes smartLocksTableKeysAndAttributes = new TableKeysAndAttributes(smartLocksTableName);
		        	smartLocksTableKeysAndAttributes.addHashOnlyPrimaryKeys("deviceID",
		        			"SL001",
		        			"SL002",
		        			"SL003");
		        	TableKeysAndAttributes smartUltraSensorsTableKeysAndAttributes = new TableKeysAndAttributes(smartUltraSensorsTableName);
		        	smartUltraSensorsTableKeysAndAttributes.addHashOnlyPrimaryKeys("deviceID",
		        			"MS001",
		        			"MS002",
		        			"MS003");
		        	BatchGetItemOutcome outcome = DDB.batchGetItem(smartLocksTableKeysAndAttributes,
		        			smartUltraSensorsTableKeysAndAttributes);
		        	for (String turn : outcome.getTableItems().keySet()) {
			        	System.out.println("------------------------------------------------");
		        	    System.out.println("ACTIVE " + turn + " DEVICES:");
		        	    List<Item> items = outcome.getTableItems().get(turn);
		        	    for (Item item : items) {
		        	    	if (turn == smartLocksTableName) {
		        	    		String condition = "LOCKED";
		        	    		if (item.getInt("lock_condition") != 1)
		        	    			condition = "UNLOCKED";
		        	    		System.out.println("------------------------------------------------");
		        	    		System.out.println("Device ID: " + item.get("deviceID") + " | Location: " + item.get("location") + " "
		        	    				+ "| Current Status: " + condition);
		        	    		if (condition == "UNLOCKED") {
		        	    		    
		        	    		    BODY = "Smart Lock with Device ID " + item.get("deviceID") + " at location ["
		        	    		    		+ item.get("location") + "] is currently UNLOCKED.";
		        	    		    System.out.println("Disturbances Detected! " + BODY);
		        	                System.out.println("Notifying Manager...");
		        	                
		        	                SUBJECT = "Smart Security Alert: UNLOCK";
		        	    		    subject = new Content().withData(SUBJECT);
		        	    	        textBody = new Content().withData(BODY);
		        	    	        body = new Body().withText(textBody);
		        	    	        message = new Message().withSubject(subject).withBody(body);
		        	    	        request = new SendEmailRequest().withSource(FROM).withDestination(destination).withMessage(message);
		        	    	        client.sendEmail(request);
		        	                
		        	                System.out.println("Manager has been notified via email!");
		        	                System.out.println("------------------------------------------------");
		        	    		}
		        	    	}
		        	    	if (turn == smartUltraSensorsTableName) {
		        	    		String condition = "DISTURBED";
		        	    		if (item.getInt("default_distance") != item.getInt("current_distance"))
		        	    			condition = "UNDISTURBED";
		        	    		System.out.println("------------------------------------------------");
		        	    		System.out.println("Device ID: " + item.get("deviceID") + " | Location: " + item.get("location") +" "
		        	    				+ "| Current Status: " + condition);
		        	    		if (condition == "DISTURBED") {
		        	    			SUBJECT = "Smart Security Alert: MOTION DISTURBANCE";
		        	    		    BODY = "Smart Ultrasonic Sensor with Device ID " + item.get("deviceID") + " at location ["
		        	    		    		+ item.get("location") + "] has been DISTURBED.";
		        	    		    
		        	    		    System.out.println("Disturbances Detected! " + BODY);
		        	                System.out.println("Notifying Manager...");
		        	                
		        	    		    subject = new Content().withData(SUBJECT);
		        	    	        textBody = new Content().withData(BODY);
		        	    	        body = new Body().withText(textBody);
		        	    	        message = new Message().withSubject(subject).withBody(body);
		        	    	        request = new SendEmailRequest().withSource(FROM).withDestination(destination).withMessage(message);
		        	    	        client.sendEmail(request);
		        	                
		        	                System.out.println("Manager has been notified via email!");
		        	                System.out.println("------------------------------------------------");
		        	    		}
		        	    	}
		        	    }
		        	}
		        	System.out.println("------------------------------------------------");
		        	System.out.println("------------------------------------------------");
		        	System.out.println();
					break;
				case 2:
					String device;
					DeleteItemOutcome reset;
					while (operation != 7) {
						System.out.print("Select a Device to Reset\n"
								+ "[1] SL001\n"
								+ "[2] SL002\n"
								+ "[3] SL003\n"
								+ "[4] MS001\n"
								+ "[5] MS002\n"
								+ "[6] MS003\n"
								+ "[0] Cancel\n");
						System.out.println();
						operation = input.nextInt();
						switch (operation) {
							case 1:
								device = "SL001";
								reset = smartLocksTable.deleteItem("deviceID", device);
								System.out.println("Device "+ device +" has been reset!\n");
								break;
							case 2:
								device = "SL002";
								reset = smartLocksTable.deleteItem("deviceID", device);
								System.out.println("Device "+ device +" has been reset!\n");
								break;
							case 3:
								device = "SL003";
								reset = smartLocksTable.deleteItem("deviceID", device);
								System.out.println("Device "+ device +" has been reset!\n");
								break;
							case 4:
								device = "MS001";
								reset = smartUltraSensorsTable.deleteItem("deviceID", device);
								System.out.println("Device "+ device +" has been reset!\n");
								break;
							case 5:
								device = "MS002";
								reset = smartUltraSensorsTable.deleteItem("deviceID", device);
								System.out.println("Device "+ device +" has been reset!\n");
								break;
							case 6:
								device = "MS003";
								reset = smartUltraSensorsTable.deleteItem("deviceID", device);
								System.out.println("Device "+ device +" has been reset!\n");
								break;
							case 0:
								operation = 7;
								break;
							default:
								break;
						}
					}
					//operation = -1;
					break;
				case 0:
					operation = 0;
					break;
			}
        }
        input.close();
		System.out.print("Shut Down Complete!\n");
	}
}
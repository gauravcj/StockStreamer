package com.gauravcj.sensex.gen;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.json.simple.JSONObject;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.producer.Attempt;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaFunctionHandler implements RequestHandler<Object, String> {
	ArrayList<String> stockList = new ArrayList<String>();
	private final String STREAMNAME = "stockprices";

	public LambdaFunctionHandler() {
		String[] stockArray = { "Phoenix", "Mordor", "Isengard", "Griffindor Inc", "42 Life Solutions", "Shire Ltd",
				"Qarth Traders", "Aragorn traders", "Orc Foundry", "Weasleys Wizard Wheezes", "Lannisters corp",
				"Tyrell travels", "Stark upholstry", "Greyjoy Freightworks" };
		stockList.addAll(Arrays.asList(stockArray));
	}

	@Override
	public String handleRequest(Object input, Context context) {
		context.getLogger().log("Input: " + input);
		try {
			this.updateRates(context);
		} catch (InterruptedException | ExecutionException e) {
			context.getLogger().log("Error" + e.getMessage());
			e.printStackTrace();
		}
		return "Executed!";
	}

	
	public void updateRates(Context context) throws InterruptedException, ExecutionException {
		KinesisProducerConfiguration config = new KinesisProducerConfiguration().setRecordMaxBufferedTime(100)
				.setMaxConnections(20).setRequestTimeout(300).setRegion("ap-south-1");

		final KinesisProducer kinesis = new KinesisProducer(config);

		config.setCredentialsProvider(new DefaultAWSCredentialsProviderChain());

		Random r = new Random();
		int updateStockIndex = 0;
		int i = 0;

		List<Future<UserRecordResult>> putFutures = new LinkedList<Future<UserRecordResult>>();
		long startTime = new Date().getTime();

		int runForSeconds = 300;
		long now = new Date().getTime();
		
		while (now - startTime <= runForSeconds * 1000) {
			updateStockIndex = r.nextInt(stockList.size());
			ByteBuffer data;
			try {
				JSONObject obj = new JSONObject();
				obj.put("stockname", stockList.get(updateStockIndex));
				obj.put("stockvalue", (r.nextInt(500000) / 100.0));

				data = ByteBuffer.wrap(obj.toJSONString().getBytes("UTF-8"));
				// doesn't block
				putFutures.add(kinesis.addUserRecord(STREAMNAME, stockList.get(updateStockIndex), data));
				Thread.sleep(10);
				now = new Date().getTime();
				++i;
				//context.getLogger().log(i + "::" + obj.toJSONString());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

		}
		
		context.getLogger().log("Total puts ::"+ i);
		
		System.out.println("..... All Done, shutting down. ");
		// Wait for puts to finish and check the results
		for (Future<UserRecordResult> f : putFutures) {
			UserRecordResult result = f.get(); // this does block
			if (result.isSuccessful()) {
				//System.out.println("Put record into shard " + result.getShardId());
			} else {
				for (Attempt attempt : result.getAttempts()) {
					System.err.println(attempt);
				}
			}
		}

		System.out.println(kinesis.getMetrics());
	}

}

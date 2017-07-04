package com.gauravcj.lambda.sensexkinesisresponder;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.kinesis.deagg.RecordDeaggregator;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;

import redis.clients.jedis.Jedis;

public class LambdaFunctionHandler implements RequestHandler<KinesisEvent, Integer> {
	Jedis jedis = null;
	JSONParser parser = new JSONParser();
	
	@Override
	public Integer handleRequest(KinesisEvent event, Context context) {
		if (jedis == null) {
			jedis = new Jedis("sensexdb.7ouz7i.0001.aps1.cache.amazonaws.com");
		}

		context.getLogger().log(" Received Input: " + event);

		// Your User Record Processing Code Here!
		RecordDeaggregator.stream(event.getRecords().stream(), userRecord -> {
			// Your User Record Processing Code Here!
			String data = new String(userRecord.getData().array());
			
			context.getLogger().log(String.format("Sensex Data: %s :::Processing UserRecord %s (%s:%s)",data, userRecord.getPartitionKey(),
					userRecord.getSequenceNumber(), userRecord.getSubSequenceNumber()));
			
			try {
				JSONObject dataObj = (JSONObject) parser.parse(data);
				String stockName = (String) dataObj.get("stockname");
				Double stockValue = (Double) dataObj.get("stockvalue");
				
				jedis.publish("sensex", data);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
		});

		return event.getRecords().size();
	}
}

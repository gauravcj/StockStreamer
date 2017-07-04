package com.gauravcj.demo;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

@ApplicationScoped
@ServerEndpoint("/websocket")
public class SensexPriceSocket {
	Jedis jedis = null;
	public SensexPriceSocket(){

	}
	
	   @OnMessage
	    public void onMessage(String message, Session session) throws IOException,
	            InterruptedException {
	        System.out.println("User input: " + message);
	        session.getBasicRemote().sendText("Instance id"+this.toString());
			if (jedis == null) {
				jedis = new Jedis("sensexdb.7ouz7i.0001.aps1.cache.amazonaws.com");
			}
			JedisPubSub jedisPubSub = setupSubscriber(session);
			jedis.subscribe(jedisPubSub, "sensex");
	    }
	 
	    @OnOpen
	    public void onOpen() {
	        System.out.println("Client connected");
	    }
	 
	    @OnClose
	    public void onClose() {
	        System.out.println("Connection closed");
	    }
	    
		
	    private JedisPubSub setupSubscriber(final Session session) {
			final JedisPubSub jedisPubSub = new JedisPubSub() {
				@Override
				public void onUnsubscribe(String channel, int subscribedChannels) {
					System.out.println("onUnsubscribe");
				}

				@Override
				public void onSubscribe(String channel, int subscribedChannels) {
					System.out.println("onSubscribe");
				}

				@Override
				public void onPUnsubscribe(String pattern, int subscribedChannels) {
				}

				@Override
				public void onPSubscribe(String pattern, int subscribedChannels) {
				}

				@Override
				public void onPMessage(String pattern, String channel, String message) {
				}

				@Override
				public void onMessage(String channel, String message) {
					try {
						session.getBasicRemote().sendText(message);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			return jedisPubSub;
		}
	    
	    
}

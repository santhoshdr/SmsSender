package net.drs.myapp.mqservice;

public interface IRabbitMqService {
	
	
	void publishSMSMessage(String smsmessage, String number);
	
	

}

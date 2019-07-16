package net.drs.myapp.service;

import net.drs.myapp.mqservice.NotificationRequest;

public interface ISendNotification {
	
	public void sendSMSNotification(NotificationRequest notificationRequest);
	
	public void sendEmailNotification(NotificationRequest notificationRequest);

}

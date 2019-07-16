package net.drs.myapp.service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.validation.constraints.Email;

import net.drs.myapp.mqservice.NotificationRequest;

import org.springframework.stereotype.Repository;

@Repository("sendNotification")
@Transactional
public class SendNotificationImpl implements ISendNotification {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public void sendSMSNotification(NotificationRequest notificationRequest) {
		
		// Send SMS Notification Here.............
		
		
		
		int result = entityManager.createNativeQuery("update email_notification set EMAIL_MESSAGE_SENT = 'true' where id=:id").setParameter("id", notificationRequest.getNotificationId()).executeUpdate();
		System.out.println("RESULT " + result);
	
	}

	@Override
	public void sendEmailNotification(NotificationRequest notificationRequest) {
		// TODO Auto-generated method stub

	}

}

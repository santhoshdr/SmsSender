package net.drs.myapp.service;

import net.drs.common.notifier.NotificationRequest;

public interface ISendNotification {

    public void sendSMSNotification(NotificationRequest notificationRequest) throws Exception;

    public void sendEmailNotification(NotificationRequest notificationRequest) throws Exception;

}

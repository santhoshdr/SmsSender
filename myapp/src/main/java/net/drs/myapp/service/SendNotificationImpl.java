package net.drs.myapp.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.sun.mail.smtp.SMTPTransport;

import net.drs.common.notifier.NotificationDataConstants;
import net.drs.common.notifier.NotificationRequest;
import net.drs.common.notifier.NotificationTemplate;
import net.drs.myapp.model.Email;

@Repository("sendNotification")
@Transactional
public class SendNotificationImpl implements ISendNotification {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${email.from.address}")
    private String fromEmailAddress;

    @Value("${email.new.registration.message}")
    private String newRegistrationMessage;

    @Value("${email.forgot.password.message}")
    private String forgotPasswordMessage;

    @Value("${email.change.password.message}")
    private String changePasswordMessage;

    @Override
    public void sendSMSNotification(NotificationRequest notificationRequest) {
        int result = entityManager.createNativeQuery("update email_notification set EMAIL_MESSAGE_SENT = 'true' where id=:id").setParameter("id", notificationRequest.getNotificationId())
                .executeUpdate();
    }

    @Override
    public void sendEmailNotification(NotificationRequest notificationRequest) throws Exception {

        NotificationTemplate template = notificationRequest.getNotificationTemplate();
        String temperoryActivationString = notificationRequest.getData().get(NotificationDataConstants.TEMPERORY_ACTIVATION_STRING);
        String user_name = notificationRequest.getData().get(NotificationDataConstants.USER_NAME);

        String emailmessage;
        switch (template) {
        case NEW_REGISTRATION:
            emailmessage = String.format(newRegistrationMessage, user_name, temperoryActivationString);
            notificationRequest.setEmailContent(emailmessage);
            break;
        case FORGOT_PASSWORD:
            break;
        case CHANGE_PASSWORD:
            break;
        }
        int result = sendEmail(notificationRequest);
        Email email = entityManager.find(net.drs.myapp.model.Email.class, notificationRequest.getNotificationId());
        email.setNeedtoSendEmail(false);
        email.setUpdatedBy("SYSTEM");
        email.setEmailMessageSent(notificationRequest.getEmailContent());
        email.setUpdatedDate(new java.sql.Date(System.currentTimeMillis()));
        email.setEmailresponse(Integer.toString(result));
        entityManager.persist(email);
    }

    private int sendEmail(NotificationRequest notificationRequest) throws Exception {
        Properties prop = System.getProperties();
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop, null);
        Message msg = new MimeMessage(session);
        SMTPTransport t = null;
        try {
            msg.setFrom(new InternetAddress("fromEmailAddress"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("santhoshdesikulkarni@gmail.com", false));
            msg.setSubject("NEW REGISTRATION");
            msg.setDataHandler(new DataHandler(new HTMLDataSource(notificationRequest.getEmailContent())));
            t = (SMTPTransport) session.getTransport("smtp");
            t.connect("smtp.gmail.com", "myappneed@gmail.com", "Santhosh@123");
            // https://myaccount.google.com/lesssecureapps?pli=1 is turned on .
            // this should be off for security
            t.sendMessage(msg, msg.getAllRecipients());
            return t.getLastReturnCode();
        } catch (AddressException e) {
            throw new Exception("Unable to send Email", e);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new Exception("Unable to send Email", e);
        } finally {
            t.close();
        }
    }

    static class HTMLDataSource implements DataSource {

        private String html;

        public HTMLDataSource(String htmlString) {
            html = htmlString;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (html == null)
                throw new IOException("html message is null!");
            return new ByteArrayInputStream(html.getBytes());
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("This DataHandler cannot write HTML");
        }

        @Override
        public String getContentType() {
            return "text/html";
        }

        @Override
        public String getName() {
            return "HTMLDataSource";
        }
    }

}

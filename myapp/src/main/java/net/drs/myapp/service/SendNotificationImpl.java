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

import net.drs.common.notifier.NotificationRequest;

@Repository("sendNotification")
@Transactional
public class SendNotificationImpl implements ISendNotification {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${email.from.address}")
    private String fromEmailAddress;

    @Override
    public void sendSMSNotification(NotificationRequest notificationRequest) {
        int result = entityManager.createNativeQuery("update email_notification set EMAIL_MESSAGE_SENT = 'true' where id=:id").setParameter("id", notificationRequest.getNotificationId())
                .executeUpdate();
        System.out.println("RESULT " + result);
    }

    @Override
    public void sendEmailNotification(NotificationRequest notificationRequest) throws Exception {
        int result = sendEmail(notificationRequest);
        /*
         * Email email = entityManager.find(net.drs.myapp.model.Email.class,
         * notificationRequest.getNotificationId());
         * email.setNeedtoSendEmail(false); email.setUpdatedBy("SYSTEM");
         * email.setUpdatedDate(new java.sql.Date(System.currentTimeMillis()));
         * email.setEmailresponse(Integer.toString(result));
         * entityManager.persist(email);
         */
        System.out.println("RESULT " + result);
    }

    private int sendEmail(NotificationRequest notificationRequest) throws Exception {

        String toAddress = notificationRequest.getEmailid();
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
            msg.setSubject("Email subject");
            msg.setDataHandler(new DataHandler(new HTMLDataSource("<h1>Hello Java Mail \\n ABC123</h1>")));
            t = (SMTPTransport) session.getTransport("smtp");
            t.connect("smtp.gmail.com", "myappn22eed@gmail.com", "Santhosh@123");
            // https://myaccount.google.com/lesssecureapps?pli=1 is turned on .
            // this should be off for security
            t.sendMessage(msg, msg.getAllRecipients());
            if (t.getLastReturnCode() == 250) {
                System.out.println("Email has been sent successfully");
            } else {
                System.out.println("Email has not been sent successfully" + t.getLastReturnCode());
            }
        } catch (AddressException e) {
            throw new Exception("Unable to send Email", e);
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            t.close();
        }
        return t.getLastReturnCode();
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

package net.drs.myapp.mqservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Envelope;

import net.drs.common.notifier.NotificationRequest;
import net.drs.myapp.service.ISendNotification;

//@Component
//@Repository("rabbitMqService")
//@Transactional
public class NotifierRabbitMqService2 implements IRabbitMqService {

    private static final Logger LOG = LoggerFactory.getLogger(NotifierRabbitMqService2.class);

    private static final String EXCHANGE_NAME = "pub-sub-queue";

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Autowired
    private ISendNotification sendNotification;

    private static final String MQ_HOST = "142.93.223.247";

    private List<Channel> channels = new ArrayList<>();

    private static final Map<String, Object> QUEUE_ARGS = new HashMap<>();

    @PostConstruct
    public void postConsrtuct() {
        MqUtils.manageCheckedExceptions(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                initializeMq();
                return null;
            }
        });
    }

    public void initializeMq() {
        try {
            receiveSMSMQMessage();
        } catch (Exception e) {
            LOG.error("Error Occurred during initialization of channel");
        }
    }

    @Override
    public void publishSMSMessage(String smsmessage, String number) {

        LOG.info("Publishing message to MQ: SMS message {} , number {} ", smsmessage, number);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(MQ_HOST);
        factory.setPort(5672);
        Map<String, String> message = new HashMap<String, String>();
        message.put(number, smsmessage);
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            channel.basicPublish(EXCHANGE_NAME, "", null, message.toString().getBytes());
        } catch (IOException | TimeoutException e) {
            LOG.error("Exception Occurred while creating MQ connection");
            e.printStackTrace();
        }
    }

    public String receiveSMSMQMessage() throws Exception {

        try {
            System.out.println(" Inside receiveSMSMQMessage ");
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(MQ_HOST);
            factory.setPort(15672);
            factory.setUsername("appuser");
            factory.setPassword("appuser");
            factory.setVirtualHost("/");
            Connection connection;

            connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NAME, "");

            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                delivery.getBody();
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Received '" + message + "'");

                NotificationRequest notificationReq = DEFAULT_OBJECT_MAPPER.readValue(message, NotificationRequest.class);

                try {
                    sendNotification.sendSMSNotification(notificationReq);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                System.out.println(notificationReq.getNotificationId());
                System.out.println(notificationReq.getEmailid());
                System.out.println(notificationReq.getTemplate());
            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
            });

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /*
     * private final class SmsMqConsumer extends DefaultConsumer {
     * 
     * private SmsMqConsumer(Channel channel) { super(channel); }
     * 
     * @Override public void handleDelivery(String consumerTag, Envelope
     * envelope, AMQP.BasicProperties props, byte[] body) throws IOException {
     * 
     * }; } }
     */
    public static void main(String args[]) {
        NotifierRabbitMqService2 mq = new NotifierRabbitMqService2();
        mq.publishSMSMessage("hi", "asdasd");
    }

    private final class DmMqConsumer extends DefaultConsumer {

        private DmMqConsumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties props, byte[] body) throws IOException {

            MqUtils.manageAcknoledgement(envelope, getChannel(), new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    String event = new String(body, MqStatics.UTF_8);
                          return null;
                }
            });
        }
    }

    @Override
    public void publishSMSMessage(NotificationRequest notificationReq) {
        // TODO Auto-generated method stub

    }

}

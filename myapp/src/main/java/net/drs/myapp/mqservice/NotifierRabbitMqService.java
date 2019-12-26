package net.drs.myapp.mqservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import net.jodah.lyra.Connections;
import net.jodah.lyra.config.Config;
import net.jodah.lyra.config.ConfigurableConnection;
import net.jodah.lyra.config.RecoveryPolicies;
import net.jodah.lyra.config.RetryPolicies;

@Component
@Repository("notifierRabbitMqService")
@Transactional
public class NotifierRabbitMqService implements IRabbitMqService {

    private static final Logger LOG = LoggerFactory.getLogger(NotifierRabbitMqService.class);

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private static final String EXCHANGE_NAME = "pub-sub-queue";

    private static final String MQ_HOST = "142.93.223.247";

    private static final String NOTIFICATION_QUEUE = "sms-email";

    private ExecutorService executorService;

    private List<Channel> channels = new ArrayList<>();

    @Autowired
    private ISendNotification sendNotification;
    
    @PostConstruct
    private void postConsrtuct() {

        MqUtils.manageCheckedExceptions(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                initializeMq();

                manageChannelStartup();
                return null;
            }
        });
    }

    protected void manageChannelStartup() throws Exception {

        Channel oneChannel = getOneChannel();
        oneChannel.queueDeclare(NOTIFICATION_QUEUE, MqStatics.DURABLE, MqStatics.EXCLUSIVE, MqStatics.AUTO_DELETE, MqStatics.QUEUE_DLX_ARGS);
        oneChannel.exchangeDeclare(NOTIFICATION_QUEUE, MqStatics.TYPE, MqStatics.DURABLE);

        for (Channel channel : channels) {
            channel.basicQos(MqStatics.PREFETCH_COUNT, MqStatics.PREFETCH_GLOBAL);
            channel.basicConsume(MqStatics.NOTIFICATION_QUEUE, MqStatics.AUTO_ACK, new DmMqBatchResponseConsumer(channel));
        }

    }

    /**
     * Return a channel from the pool
     * 
     * @return one channel
     * @throws Exception
     */
    private Channel getOneChannel() throws Exception {
        Iterator<Channel> iterator = channels.iterator();
        while (iterator.hasNext()) {
            Channel channel = iterator.next();
            if (channel.isOpen()) {
                return channel;
            }
        }
        throw new Exception("Cannot get one active RabbitMQ Channel.");
    }

    public void initializeMq() throws Exception {
        try {
            Config config = new Config()
                    // Initial connection
                    .withConnectRetryPolicy(RetryPolicies.retryAlways())
                    // Recovery connection
                    .withRecoveryPolicy(RecoveryPolicies.recoverAlways())
                    // Initial channel
                    .withChannelRetryPolicy(RetryPolicies.retryAlways())
                    // Channel recovery
                    .withChannelRecoveryPolicy(RecoveryPolicies.recoverAlways())//
                    // Recover any consumers, exchanges and queues
                    .withConsumerRecovery(true) //
                    .withExchangeRecovery(true) //
                    .withQueueRecovery(true);

            ConnectionFactory cf = new ConnectionFactory();

            cf.setHost(MQ_HOST);
            cf.setPort(5672);
            cf.setVirtualHost("/");
            cf.setUsername("appuser");
            cf.setPassword("appuser");
            cf.setAutomaticRecoveryEnabled(false);
            executorService = Executors.newFixedThreadPool(10);
            cf.setSharedExecutor(executorService);

            for (int i = 0; i < 10; i++) {
                ConfigurableConnection connection = Connections.create(cf, config);
                for (int j = 0; j < 5; j++) {
                    channels.add(connection.createChannel());
                }
            }
            // receiveSMSMQMessage();
        } catch (Exception e) {
            LOG.error("Error Occurred during initialization of channel");
            throw e;
        }
    }

    @Override
    public void publishSMSMessage(NotificationRequest notificationReq) {

        byte[] data = null;
        try {
            data = DEFAULT_OBJECT_MAPPER.writeValueAsBytes(notificationReq);
        } catch (JsonProcessingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(MQ_HOST);
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            channel.basicPublish(EXCHANGE_NAME, "", null, data);
        } catch (IOException | TimeoutException e) {
            LOG.error("Exception Occurred while creating MQ connection");
            e.printStackTrace();
        }
    }

    private final class DmMqBatchResponseConsumer extends DefaultConsumer {

        private DmMqBatchResponseConsumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            MqUtils.manageAcknoledgement(envelope, getChannel(), new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    String event = new String(body, MqStatics.UTF_8);
                    LOG.debug("Receiving Load Response:{}_Consumer tag: {}{}_Envelope: {}{}_Props: {}{}_Data: {}", MqStatics.CR, // Message
                            consumerTag, MqStatics.CR, // Exchange
                            DEFAULT_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(envelope), MqStatics.CR, // Envelop
                            DEFAULT_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(properties), MqStatics.CR, // Props
                            DEFAULT_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString("")); // Data
                     
                    NotificationRequest notificationRequest = DEFAULT_OBJECT_MAPPER.readValue(event, NotificationRequest.class);
                    sendNotification.sendEmailNotification(notificationRequest);
                    return null;
                }
            });
        }
    }

    public String receiveSMSMQMessage() {

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(MQ_HOST);
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
            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
            });

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
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

        NotifierRabbitMqService mq = new NotifierRabbitMqService();
     //   mq.publishSMSMessage(new NotificationRequest(123L, "abc@sdjfd.com", "Registrationtemplate","EMAIL"));
    }

    @Override
    public void publishSMSMessage(String smsmessage, String number) {
        // TODO Auto-generated method stub
        
    }

}

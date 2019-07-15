package net.drs.myapp.mqservice;

import java.io.IOException;
import java.util.concurrent.Callable;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public final class MqUtils {

	private MqUtils() {
		// Hide implicit constructor
	}

	public static <V> void manageCheckedExceptions(Callable<V> callable) {
		try {
			callable.call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static <V> void manageAcknoledgement(Envelope envelope,
			Channel channel, Callable<V> callable) throws IOException {
		boolean acknowledge = false;
		try {
			callable.call();
			acknowledge = true;
		} catch (Exception e) {
		} finally {
			if (acknowledge) {
				channel.basicAck(envelope.getDeliveryTag(), MqStatics.MULTIPLE);
			} else {
				// Wait 1s before Non Acknowledge the message
				// Simulate a pseudo delayed re-delivery
				MqUtils.sleep(MqStatics.REDELIVERY_TIME);
				channel.basicNack(envelope.getDeliveryTag(),
						MqStatics.MULTIPLE, MqStatics.REQUEUE);
			}
		}
	}

}

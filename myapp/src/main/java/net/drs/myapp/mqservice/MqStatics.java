package net.drs.myapp.mqservice;

public class MqStatics {

	
	 private MqStatics() {
	        // Hide public constructor
	    }

	    // Connection configuration
	    public static final int ABORT_TIMEOUT = 1000; // 1s

	    // Channel Configuration
	    public static final int CHANNEL_POOL_SIZE = 50;

	    // Queues configuration
	    public static final String DM = "dm";
	    public static final String TYPE = "direct";
	    public static final boolean DURABLE = true;
	    public static final boolean EXCLUSIVE = false;
	    public static final boolean AUTO_DELETE = false;
	    public static final int DELIVERY_MODE = 2; // Persistent
	    public static final int PRIORITY = 0; // Default
	    public static final boolean REQUEUE = true; // Re-queue on failure
	    public static final boolean MULTIPLE = true; // No multiple Ack
	    public static final long REDELIVERY_TIME = 1000L; // 1s
	    public static final boolean AUTO_ACK = false;
	    public static final int PREFETCH_COUNT = 1;
	    public static final boolean PREFETCH_GLOBAL = false;
	    // Dead Letter
	    public static final String DLX = "dm-dlx";
	    public static final String DLX_TYPE = "fanout";
	    public static final String DLX_ROUTING_KEY = "#";
	    public static final String DLX_PROPERTY = "x-dead-letter-exchange";
	    public static final String DLX_REASON_PROPERTY = "x-first-death-reason";
	    public static final String DLX_REASON_EXPIRED = "expired";

	    // Messages configuration
	    public static final String ENCODING = "UTF-8";
	    public static final String CONTENT_TYPE = "application/json";
	    public static final String CR = System.getProperty("line.separator");

}

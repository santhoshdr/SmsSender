package net.drs.myapp.mqservice;

import java.io.Serializable;

public class NotificationRequest  implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 2358050464685162683L;
    
    private Long notificationId;

    private String emailid;
    
    private String template;
    
    
    public NotificationRequest(Long notificationId, String emailid, String template) {
        super();
        this.notificationId = notificationId;
        this.emailid = emailid;
        this.template = template;
    }

    public NotificationRequest() {
		super();
	}

	public String getEmailid() {
        return emailid;
    }

    public void setEmailid(String emailid) {
        this.emailid = emailid;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }


    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }
    

    
    
    
}

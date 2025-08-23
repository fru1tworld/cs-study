package org.kafkainaction.model;

import java.io.Serializable;

public class Alert implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private int alertId;
	private String stageId;
	private String alertLevel;
	private String alertMessage;

	public Alert(int alertId, String stageId, String alertLevel, String alertMessage) {
		this.alertId = alertId;
		this.stageId = stageId;
		this.alertLevel = alertLevel;
		this.alertMessage = alertMessage;
	}

	public int getAlertId() {
		return alertId;
	}

	public String getStageId() {
		return stageId;
	}

	public void setStageId(String stageId) {
		this.stageId = stageId;
	}

	public String getAlertLevel() {
		return alertLevel;
	}

	public String getAlertMessage() {
		return alertMessage;
	}
}

package com.ultron.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Opt-in connector credentials (L5 — Section 11.7). Every field is blank by default → each
 * connector reports "not connected" and stays inert until the owner supplies a token via env.
 * Zero secrets in code (Section 4); these bind from {@code ULTRON_CONNECTORS_*} env vars.
 */
@Component
@ConfigurationProperties(prefix = "ultron.connectors")
public class ConnectorProperties {

    private String gmailToken = "";
    private String calendarToken = "";
    private String notionToken = "";
    private String slackToken = "";
    private String spotifyToken = "";
    private String homeassistantUrl = "";
    private String homeassistantToken = "";
    // Twilio is a contact-touching extension point (Section 3) — present but never auto-armed.
    private String twilioAccountSid = "";
    private String twilioAuthToken = "";
    private boolean twilioLiveEnabled = false;

    public String getGmailToken() { return gmailToken; }
    public void setGmailToken(String v) { this.gmailToken = v; }
    public String getCalendarToken() { return calendarToken; }
    public void setCalendarToken(String v) { this.calendarToken = v; }
    public String getNotionToken() { return notionToken; }
    public void setNotionToken(String v) { this.notionToken = v; }
    public String getSlackToken() { return slackToken; }
    public void setSlackToken(String v) { this.slackToken = v; }
    public String getSpotifyToken() { return spotifyToken; }
    public void setSpotifyToken(String v) { this.spotifyToken = v; }
    public String getHomeassistantUrl() { return homeassistantUrl; }
    public void setHomeassistantUrl(String v) { this.homeassistantUrl = v; }
    public String getHomeassistantToken() { return homeassistantToken; }
    public void setHomeassistantToken(String v) { this.homeassistantToken = v; }
    public String getTwilioAccountSid() { return twilioAccountSid; }
    public void setTwilioAccountSid(String v) { this.twilioAccountSid = v; }
    public String getTwilioAuthToken() { return twilioAuthToken; }
    public void setTwilioAuthToken(String v) { this.twilioAuthToken = v; }
    public boolean isTwilioLiveEnabled() { return twilioLiveEnabled; }
    public void setTwilioLiveEnabled(boolean v) { this.twilioLiveEnabled = v; }

    public static boolean set(String v) {
        return v != null && !v.isBlank();
    }
}

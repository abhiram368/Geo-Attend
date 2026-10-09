package com.example.geoattend;

public class CalendarDay {
    private String dayNumber; // "1", "2", etc. (Empty string for blank trailing padding days)
    private String subtext;   // "8h 30m", "Leave", etc.
    private String status;    // "PRESENT", "LEAVE", "HOLIDAY", "DEFAULT"
    private boolean isToday;

    public CalendarDay(String dayNumber, String subtext, String status, boolean isToday) {
        this.dayNumber = dayNumber;
        this.subtext = subtext;
        this.status = status;
        this.isToday = isToday;
    }

    public String getDayNumber() { return dayNumber; }
    public String getSubtext() { return subtext; }
    public String getStatus() { return status; }
    public boolean isToday() { return isToday; }
}
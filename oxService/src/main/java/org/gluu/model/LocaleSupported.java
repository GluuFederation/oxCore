package org.gluu.model;

public class LocaleSupported {
	
	public LocaleSupported() {
		super();
	}
	private String locale;
	private String displayName;
	
	public LocaleSupported(String locale, String displayName) {
		super();
		this.locale = locale;
		this.displayName = displayName;
	}
	
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}

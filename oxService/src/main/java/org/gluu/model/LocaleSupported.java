package org.gluu.model;

public class LocaleSupported {	
	
	private String locale;
	private String displayName;
	
	public LocaleSupported() {
	}
	
	public LocaleSupported(String locale, String displayName) {
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

	@Override
	public String toString() {
		return "LocaleSupported [locale=" + locale + ", displayName=" + displayName + "]";
	}

}

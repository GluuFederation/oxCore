package org.gluu.model;

import java.util.Objects;

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
	public int hashCode() {
		return Objects.hash(displayName, locale);
	}

	@Override
	public String toString() {
		return "LocaleSupported [locale=" + locale + ", displayName=" + displayName + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocaleSupported other = (LocaleSupported) obj;
		return Objects.equals(displayName, other.displayName) && Objects.equals(locale, other.locale);
	}

}

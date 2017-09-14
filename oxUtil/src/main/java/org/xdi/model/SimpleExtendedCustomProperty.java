package org.xdi.model;

import org.codehaus.jackson.annotate.JsonIgnore;

public class SimpleExtendedCustomProperty extends SimpleCustomProperty {

	private static final long serialVersionUID = 7413216569115979793L;

	@JsonIgnore
	private boolean hide;
	
	
	public SimpleExtendedCustomProperty() {
		super();
		
	}
	
	public SimpleExtendedCustomProperty(String value1, String value2) {
		super(value1, value2);
	}
	
	public SimpleExtendedCustomProperty(String value1, String value2, boolean hide) {
		super(value1, value2);
		this.hide = hide;
	}
	
	public SimpleExtendedCustomProperty(String p_value1, String p_value2, String p_description) {
		super(p_value1, p_value2, p_description);
    }
	
	public SimpleExtendedCustomProperty(String p_value1, String p_value2, String p_description, boolean p_hide) {
		super(p_value1, p_value2, p_description);
		this.hide = p_hide;
    }

	public boolean gethide() {
		return hide;
	}

	public void sethide(boolean hide) {
		this.hide = hide;
	}
	
}

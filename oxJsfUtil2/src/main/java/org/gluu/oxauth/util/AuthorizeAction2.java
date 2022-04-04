package org.gluu.oxauth.util;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
//import javax.inject.Inject;
import javax.faces.context.ExternalContext;
import javax.servlet.ServletRequest;
import javax.faces.context.FacesContext;

import org.apache.commons.text.StringEscapeUtils;

@RequestScoped
@Named
public class AuthorizeAction2 {

    //@Inject - injection not working fine despite existence of beans.xml in this jar 
    //private Logger logger; = LoggerFactory.getLogger(getClass());

    public String getLoginHint() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        ServletRequest request = (ServletRequest) externalContext.getRequest();
        return StringEscapeUtils.escapeEcmaScript(request.getParameter("login_hint"));
    }

}

package org.gluu.model.custom.script.type.scim;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.type.BaseExternalType;

import javax.ws.rs.core.Response;

/**
 * @author jgomer2001
 */
public interface ScimType extends BaseExternalType {

    boolean createUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postCreateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean updateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postUpdateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean deleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postDeleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean createGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postCreateGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean updateGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postUpdateGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean deleteGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postDeleteGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean getUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean getGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes);

    boolean postSearchUsers(Object results, Map<String, SimpleCustomProperty> configurationAttributes);
    
    boolean postSearchGroups(Object results, Map<String, SimpleCustomProperty> configurationAttributes);
    
    Response manageResourceOperation(Object context, Object entity, Object payload, Map<String, SimpleCustomProperty> configurationAttributes);
    
    Response manageSearchOperation(Object context, Object searchRequest, Map<String, SimpleCustomProperty> configurationAttributes);
    
}

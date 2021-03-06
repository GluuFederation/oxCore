package org.gluu.model.custom.script.type.introspection;

import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface IntrospectionType extends BaseExternalType {

    boolean modifyResponse(Object responseAsJsonObject, Object introspectionContext);
}

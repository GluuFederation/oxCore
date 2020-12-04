package org.gluu.model.custom.script.type.revoke;

import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface RevokeTokenType extends BaseExternalType {

    boolean revoke(Object context);
}

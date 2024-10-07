package org.gluu.model.custom.script.type.authzchallenge;

import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface AuthorizationChallengeType extends BaseExternalType {

    boolean authorize(Object context);
}

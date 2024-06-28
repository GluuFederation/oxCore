/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.model.custom.script.type.fido2;

import org.gluu.model.custom.script.type.BaseExternalType;

public interface Fido2ExtensionType extends BaseExternalType {

    boolean registerAttestationStart(Object paramAsJsonNode, Object context);
    boolean registerAttestationFinish(Object paramAsJsonNode, Object context);

    boolean verifyAttestationStart(Object paramAsJsonNode, Object context);
    boolean verifyAttestationFinish(Object paramAsJsonNode, Object context);

    boolean authenticateAssertionStart(Object paramAsJsonNode, Object context);
    boolean authenticateAssertionFinish(Object paramAsJsonNode, Object context);

    boolean verifyAssertionStart(Object paramAsJsonNode, Object context);
    boolean verifyAssertionFinish(Object paramAsJsonNode, Object context);
}

/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.model.custom.script.type.token;

import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Movchan
 */
public interface UpdateTokenType extends BaseExternalType {

    boolean modifyIdToken(Object jsonWebResponse, Object tokenContext);
}

/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.model.custom.script.type.token;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * @author Yuriy Movchan
 */
public class DummyUpdateTokenType implements UpdateTokenType {

	@Override
	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
		return true;
	}

	@Override
	public int getApiVersion() {
		return 1;
	}

	@Override
	public boolean modifyIdToken(Object jwr, Object tokenContext) {
		return false;
	}

    @Override
    public boolean modifyRefreshToken(Object refreshToken, Object tokenContext) {
        return false;
    }

    @Override
    public boolean modifyAccessToken(Object accessToken, Object tokenContext) {
        return false;
    }

    @Override
    public int getRefreshTokenLifetimeInSeconds(Object tokenContext) {
        return 0;
    }

    @Override
    public int getIdTokenLifetimeInSeconds(Object context) {
        return 0;
    }

    @Override
    public int getAccessTokenLifetimeInSeconds(Object context) {
        return 0;
    }

}

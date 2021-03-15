/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Persistance Expiration
 *
 * @author Yuriy Movchan Date: 03/26/2020
 */
@Target({ FIELD })
@Retention(RUNTIME)
public @interface Expiration {
}

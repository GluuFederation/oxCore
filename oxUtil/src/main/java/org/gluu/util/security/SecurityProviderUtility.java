/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.security;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.List;

import javax.crypto.Cipher;

import org.apache.commons.io.IOUtils;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider installation utility
 *
 * @author Yuriy Movchan
 * @author madhumitas
 */
public class SecurityProviderUtility {

    public static final String DEF_JKS      = "jks";
    public static final String DEF_PKCS12   = "pkcs12";
    public static final String DEF_BCFKS    = "bcfks";

    /**
     * Security Mode Type
     * 
     * @author Sergey Manoylo
     * @version March 11, 2022 
     */
    public static enum SecurityModeType {

        JKS_SECURITY_MODE (DEF_JKS),
        PKCS12_SECURITY_MODE (DEF_PKCS12),
        BCFKS_SECURITY_MODE (DEF_BCFKS);
        
        private final String value;

        /**
         * Constructor
         * 
         * @param value string value, that defines Security Mode Type 
         */
        SecurityModeType(String value) {
            this.value = value;
        }

        /**
         * Creates/parses SecurityModeType from String value   
         * 
         * @param param string value, that defines Security Mode Type
         * @return SecurityModeType
         */
        public static SecurityModeType fromString(String param) {
            switch(param) {
            case DEF_JKS: {
                return JKS_SECURITY_MODE;
            }
            case DEF_PKCS12: {
                return PKCS12_SECURITY_MODE;
            }
            case DEF_BCFKS: {
                return BCFKS_SECURITY_MODE;
            }
            }
            return null;
        }

        /**
         * Returns a string representation of the object. In this case the parameter name for the default scope.
         */
        @Override
        public String toString() {
            return value;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SecurityProviderUtility.class);

    public static final String BC_PROVIDER_NAME = "BC";
    public static final String BC_FIPS_PROVIDER_NAME = "BCFIPS";

    public static boolean USE_FIPS_CHECK_COMMAND = false;

    private static SecurityModeType securityMode = null;

    private static Provider bouncyCastleProvider;

    private static final String BC_GENERIC_PROVIDER_CLASS_NAME = "org.bouncycastle.jce.provider.BouncyCastleProvider";
    private static final String BC_FIPS_PROVIDER_CLASS_NAME    = "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider";

    public static void installBCProvider(boolean silent) {
        String providerName = BC_PROVIDER_NAME;
        String className = BC_GENERIC_PROVIDER_CLASS_NAME;

        if (securityMode == null || securityMode == SecurityModeType.BCFKS_SECURITY_MODE) {
            boolean isFipsMode = checkFipsMode();
            if (isFipsMode) {
                LOG.info("Fips mode is enabled");

                providerName = BC_FIPS_PROVIDER_NAME;
                className = BC_FIPS_PROVIDER_CLASS_NAME;

                securityMode = SecurityModeType.BCFKS_SECURITY_MODE;
            }
            else {
                securityMode = SecurityModeType.JKS_SECURITY_MODE;
            }
        }

        try {
            installBCProvider(providerName, className, silent);
        } catch (Exception e) {
            LOG.error(
                    "Security provider '{}' doesn't exists in class path. Please deploy correct war for this environment!", providerName);
            LOG.error(e.getMessage(), e);
        }
    }

    public static void installBCProvider() {
        installBCProvider(false);
    }

    public static void installBCProvider(String providerName, String providerClassName, boolean silent) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        bouncyCastleProvider = Security.getProvider(providerName);
        if (bouncyCastleProvider == null) {
            if (!silent) {
                LOG.info("Adding Bouncy Castle Provider");
            }

            bouncyCastleProvider = (Provider) Class.forName(providerClassName).getConstructor().newInstance();
            Security.addProvider(bouncyCastleProvider);
            LOG.info("Provider '{}' with version {} is added", bouncyCastleProvider.getName(), bouncyCastleProvider.getVersionStr());
        } else {
            if (!silent) {
                LOG.info("Bouncy Castle Provider was added already");
            }
        }
    }

    /**
    * A check that the server is running in FIPS-approved-only mode. This is a part
    * of compliance to ensure that the server is really FIPS compliant
    * 
    * @return boolean value
    */
    private static boolean checkFipsMode() {
        try {
            // First check if there are FIPS provider libs
            Class.forName(BC_FIPS_PROVIDER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            LOG.trace("BC Fips provider is not available", e);
            return false;
        }

        if (USE_FIPS_CHECK_COMMAND) {
            String osName = System.getProperty("os.name");
            if (StringHelper.isNotEmpty(osName) && osName.toLowerCase().startsWith("windows")) {
                return false;
            }

            try {
                // Check if FIPS is enabled 
                Process process = Runtime.getRuntime().exec("fips-mode-setup --check");
                List<String> result = IOUtils.readLines(process.getInputStream(), StandardCharsets.UTF_8);
                if ((result.size() > 0) && StringHelper.equalsIgnoreCase(result.get(0), "FIPS mode is enabled.")) {
                    return true;
                }
            } catch (IOException e) {
                LOG.error("Failed to check if FIPS mode was enabled", e);
                return false;
            }
            return false;
        }

        return true;
    }

    /**
     * Determines if cryptography restrictions apply.
     * Restrictions apply if the value of {@link Cipher#getMaxAllowedKeyLength(String)} returns a value smaller than {@link Integer#MAX_VALUE} if there are any restrictions according to the JavaDoc of the method.
     * This method is used with the transform <code>"AES/CBC/PKCS5Padding"</code> as this is an often used algorithm that is <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#impl">an implementation requirement for Java SE</a>.
     *
     * @return <code>true</code> if restrictions apply, <code>false</code> otherwise
     * https://stackoverflow.com/posts/33849265/edit, author Maarten Bodewes
     */
    public static boolean checkRestrictedCryptography() {
        try {
            return Cipher.getMaxAllowedKeyLength("AES/CBC/PKCS5Padding") < Integer.MAX_VALUE;
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("The transform \"AES/CBC/PKCS5Padding\" is not available (the availability of this algorithm is mandatory for Java SE implementations)", e);
        }
    }

    public static String getBCProviderName() {
        return bouncyCastleProvider.getName();
    }

    public static Provider getBCProvider() {
        return bouncyCastleProvider;
    }

    public static SecurityModeType getSecurityMode() {
        return securityMode;
    }

    public static void setSecurityMode(SecurityModeType securityModeIn) {
        securityMode = securityModeIn;
    }
}

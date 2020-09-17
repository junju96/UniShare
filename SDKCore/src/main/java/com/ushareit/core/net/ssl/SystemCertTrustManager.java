package com.ushareit.core.net.ssl;

import com.ushareit.core.Logger;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Enumeration;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * A X509 trust manager implementation which asks the user about invalid
 * certificates and memorizes their decision.
 * <p>
 * The certificate validity is checked using the system default X509
 * TrustManager, creating a query Dialog if the check fails.
 * <p>
 * <b>WARNING:</b> This only works if a dedicated thread is used for
 * opening sockets!
 */
public class SystemCertTrustManager implements X509TrustManager {

    private static final String TAG = "secure.ssl.sys.tm";
    private KeyStore appKeyStore;
    private X509TrustManager defaultTrustManager;
    private X509TrustManager appTrustManager;

    /**
     * Creates an instance of the SystemCertTrustManager class that falls back to a custom TrustManager.
     *
     * @param defaultTrustManager Delegate trust management to this TM. If null, the user must accept every certificate.
     */
    public SystemCertTrustManager(X509TrustManager defaultTrustManager) {
        init();
        this.defaultTrustManager = defaultTrustManager;
    }

    /**
     * Creates an instance of the SystemCertTrustManager class using the system X509TrustManager.
     * <p>
     */
    public SystemCertTrustManager() {
        init();
        this.defaultTrustManager = getTrustManager(null);
    }

    void init() {
        appKeyStore = loadAppKeyStore();
        appTrustManager = getTrustManager(appKeyStore);
    }

    /**
     * Returns a X509TrustManager list containing a new instance of
     * TrustManagerFactory.
     * <p>
     * This function is meant for convenience only. You can use it
     * as follows to integrate TrustManagerFactory for HTTPS sockets:
     * <p>
     * <pre>
     *     SSLContext sc = SSLContext.getInstance("TLS");
     *     sc.init(null, SystemCertTrustManager.getInstanceList(),
     *         new java.security.SecureRandom());
     *     HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
     * </pre>
     */
    public static X509TrustManager[] getInstanceList() {
        return new X509TrustManager[]{new SystemCertTrustManager()};
    }

    /**
     * Get a list of all certificate aliases stored in MTM.
     *
     * @return an {@link Enumeration} of all certificates
     */
    public Enumeration<String> getCertificates() {
        try {
            return appKeyStore.aliases();
        } catch (KeyStoreException e) {
            // this should never happen, however...
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a certificate for a given alias.
     *
     * @param alias the certificate's alias as returned by {@link #getCertificates()}.
     * @return the certificate associated with the alias or <tt>null</tt> if none found.
     */
    public Certificate getCertificate(String alias) {
        try {
            return appKeyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            // this should never happen, however...
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes the given certificate from MTMs key store.
     * <p>
     * <p>
     * <b>WARNING</b>: this does not immediately invalidate the certificate. It is
     * well possible that (a) data is transmitted over still existing connections or
     * (b) new connections are created using TLS renegotiation, without a new cert
     * check.
     * </p>
     *
     * @param alias the certificate's alias as returned by {@link #getCertificates()}.
     * @throws KeyStoreException if the certificate could not be deleted.
     */
    public void deleteCertificate(String alias) throws KeyStoreException {
        appKeyStore.deleteEntry(alias);
    }

    X509TrustManager getTrustManager(KeyStore ks) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(ks);
            for (TrustManager t : tmf.getTrustManagers()) {
                if (t instanceof X509TrustManager) {
                    return (X509TrustManager) t;
                }
            }
        } catch (Exception e) {
            // Here, we are covering up errors. It might be more useful
            // however to throw them out of the constructor so the
            // embedding app knows something went wrong.
            Logger.d(TAG, "getTrustManager(" + ks + ")", e);
        }
        return null;
    }

    KeyStore loadAppKeyStore() {
        KeyStore ks;
        try {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            Logger.d(TAG, "getAppKeyStore()", e);
            return null;
        }
        try {
            ks.load(null, null);
        } catch (Exception e) {
            Logger.d(TAG, "loadAppKeyStore", e);
        }
        return ks;
    }

    // if the certificate is stored in the app key store, it is considered "known"
    private boolean isCertKnown(X509Certificate cert) {
        try {
            return appKeyStore.getCertificateAlias(cert) != null;
        } catch (KeyStoreException e) {
            return false;
        }
    }

    private static boolean isExpiredException(Throwable e) {
        do {
            if (e instanceof CertificateExpiredException)
                return true;
            e = e.getCause();
        } while (e != null);
        return false;
    }

    private static boolean isPathException(Throwable e) {
        do {
            if (e instanceof CertPathValidatorException)
                return true;
            e = e.getCause();
        } while (e != null);
        return false;
    }

    public void checkCertTrusted(X509Certificate[] chain, String authType, boolean isServer)
            throws CertificateException {
        Logger.d(TAG, "checkCertTrusted(" + Arrays.toString(chain) + ", " + authType + ", " + isServer + ")");
        try {
            Logger.d(TAG, "checkCertTrusted: trying appTrustManager");
            if (isServer)
                appTrustManager.checkServerTrusted(chain, authType);
            else
                appTrustManager.checkClientTrusted(chain, authType);
        } catch (CertificateException ae) {
            Logger.d(TAG, "checkCertTrusted: appTrustManager did not verify certificate. Will fall back to secondary verification mechanisms (if any).", ae);
            // if the cert is stored in our appTrustManager, we ignore expires
            if (isExpiredException(ae)) {
                Logger.d(TAG, "checkCertTrusted: accepting expired certificate from keystore");
                return;
            }
            if (isCertKnown(chain[0])) {
                Logger.d(TAG, "checkCertTrusted: accepting cert already stored in keystore");
                return;
            }
            try {
                if (defaultTrustManager == null) {
                    Logger.d(TAG, "No defaultTrustManager set. Verification failed, throwing " + ae);
                    throw ae;
                }
                Logger.d(TAG, "checkCertTrusted: trying defaultTrustManager");
                if (isServer)
                    defaultTrustManager.checkServerTrusted(chain, authType);
                else
                    defaultTrustManager.checkClientTrusted(chain, authType);
            } catch (CertificateException e) {
                Logger.d(TAG, "checkCertTrusted: defaultTrustManager failed", e);
            }
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        checkCertTrusted(chain, authType, false);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        checkCertTrusted(chain, authType, true);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        Logger.d(TAG, "getAcceptedIssuers()");
        return defaultTrustManager.getAcceptedIssuers();
    }

    private static String hexString(byte[] data) {
        StringBuilder si = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            si.append(String.format("%02x", data[i]));
            if (i < data.length - 1)
                si.append(":");
        }
        return si.toString();
    }

    private static String certHash(final X509Certificate cert, String digest) {
        try {
            MessageDigest md = MessageDigest.getInstance(digest);
            md.update(cert.getEncoded());
            return hexString(md.digest());
        } catch (java.security.cert.CertificateEncodingException e) {
            return e.getMessage();
        } catch (java.security.NoSuchAlgorithmException e) {
            return e.getMessage();
        }
    }

    private static void certDetails(StringBuilder si, X509Certificate c) {
        SimpleDateFormat validityDateFormater = new SimpleDateFormat("yyyy-MM-dd");
        si.append("\n");
        si.append(c.getSubjectDN().toString());
        si.append("\n");
        si.append(validityDateFormater.format(c.getNotBefore()));
        si.append(" - ");
        si.append(validityDateFormater.format(c.getNotAfter()));
        si.append("\nSHA-256: ");
        si.append(certHash(c, "SHA-256"));
        si.append("\nSHA-1: ");
        si.append(certHash(c, "SHA-1"));
        si.append("\nSigned by: ");
        si.append(c.getIssuerDN().toString());
        si.append("\n");
    }
}


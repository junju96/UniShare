package com.ushareit.core.net.ssl;

import com.ushareit.core.Logger;
import com.ushareit.core.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SpecialCertTrustManager implements X509TrustManager {

    private static final String TAG = "secure.ssl.sp.tm";

    private static final String CERT_ALIAS = "shareit";
    private String mCertName;

    private X509TrustManager appTrustManager;

    /**
     * @param certName encrypted cert file's path, in assets folder.
     */
    public SpecialCertTrustManager(String certName) {
        mCertName = certName;
        init();
    }

    private void init() {
        appTrustManager = getTrustManager(loadKeyStore());
    }

    private X509TrustManager getTrustManager(KeyStore ks) {
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

    private KeyStore loadKeyStore() {
        KeyStore ks = null;
        InputStream caInputStream = null;
        try {
            Logger.d(TAG, "loadKeyStore, cert: " + mCertName);
            if (SslCredentialStore.getInstance().hasCredential(mCertName))
                caInputStream = new ByteArrayInputStream(SslCredentialStore.getInstance().getCredential(mCertName));
            else {
                byte[] credential = SslCredentialCypher.getCredential(mCertName);
                caInputStream = new ByteArrayInputStream(credential);
                SslCredentialStore.getInstance().storeCredential(mCertName, credential);
            }

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate x509Certificate = (X509Certificate) cf.generateCertificate(caInputStream);

            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry(CERT_ALIAS, x509Certificate);
        } catch (Exception e) {
            Logger.d(TAG, "loadKeyStore", e);
        } finally {
            Utils.close(caInputStream);
        }
        return ks;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        Logger.d(TAG, "checkClientTrusted, authType: " + authType + ", cert: " + mCertName);
        if (appTrustManager == null)
            throw new CertificateException("appTrustManager is null");

        appTrustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        Logger.d(TAG, "checkServerTrusted, authType: " + authType + ", cert: " + mCertName);
        if (appTrustManager == null)
            throw new CertificateException("appTrustManager is null");

        appTrustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        Logger.d(TAG, "getAcceptedIssuers");
        if (appTrustManager == null)
            return null;

        return appTrustManager.getAcceptedIssuers();
    }
}


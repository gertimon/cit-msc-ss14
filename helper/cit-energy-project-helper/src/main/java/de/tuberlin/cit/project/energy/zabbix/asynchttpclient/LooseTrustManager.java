package de.tuberlin.cit.project.energy.zabbix.asynchttpclient;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This trust manager accepts all ssl certificates (including self singed certs).
 * @see https://github.com/AsyncHttpClient/async-http-client/commit/df6ed70e86c8fc340ed75563e016c8baa94d7e72#diff-d4856d854aa87f5f7eb534d28c6075d3
 */
public class LooseTrustManager implements X509TrustManager {

	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
	}

	public X509Certificate[] getAcceptedIssuers() {
		return new java.security.cert.X509Certificate[0];
	}
	
	public static SSLContext getSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] { new LooseTrustManager() }, new SecureRandom());
		return sslContext;
	}
}

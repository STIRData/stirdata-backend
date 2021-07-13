package com.ails.stirdatabackend.service;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

public class HttpClientCert {

	public static CloseableHttpClient createClient() throws Exception {
    	SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(new TrustStrategy() {
			    @Override
			    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			        return true;
			    }
			}).build();

    	HostnameVerifier hnv = new NoopHostnameVerifier();      
    	SSLConnectionSocketFactory sslcf = new SSLConnectionSocketFactory(sslContext, hnv);     
    	return HttpClients.custom().setSSLSocketFactory(sslcf).build();
	}
}

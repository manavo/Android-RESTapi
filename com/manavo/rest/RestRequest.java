package com.manavo.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class RestRequest {
	
	private String username;
	private String password;
	private String host;
	private int port;
	private int sslPort;
	private Handler handler;
	private ExecuteAsyncRequest asyncTask;
	
	private boolean acceptAllSslCertificates = false;

    private DefaultHttpClient httpClient;

	private boolean useSsl = true;
	
	private List<NameValuePair> data;
	
	private String userAgent = null;
    private HttpContext requestContext;

    private String contentType = null;

    public RestRequest() {
        this.requestContext = new BasicHttpContext();
        this.httpClient = this.getNewHttpClient();
        
        // Default port to be 80
        this.port = 80;
        // Default SSL port to be 443
        this.sslPort = 443;
    }

    public void setContentType(String type) {
        this.contentType = type;
    }

    public HttpContext getRequestContext() {
        return this.requestContext;
    }

    public DefaultHttpClient getHttpClient() {
        return this.httpClient;
    }
	
	public void authorize(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public void setHandler(Handler handler) {
		this.handler = handler;
	}
	
	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public void setSslPort(int port) {
		this.sslPort = port;
	}
	
	public void setSsl(boolean ssl) {
		this.useSsl = ssl;
	}
	
	public void acceptAllSslCertificates() {
		this.acceptAllSslCertificates = true;
	}
	
	public void setData(List<NameValuePair> data) {
		this.data = data;
	}
	
	public void setUserAgent(String agent) {
		this.userAgent = agent;
	}
	
	public void get(String url) {
		if (this.data.size() > 0) {
			// if we don't already have some query string parameters, add a ?
			if (url.indexOf("?") == -1) {
				url += "?";
			} else { // if query sting parameter already exist, then keep adding to them
				url += "&";
			}
			for (int i=0; i<this.data.size(); i++) {
				NameValuePair p = this.data.get(i);
				try {
					url += p.getName() + "=" + URLEncoder.encode(p.getValue(), "utf-8") + "&";
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			url = url.substring(0, url.length()-1);
		}
		HttpGet httpGet = new HttpGet(url);
		this.prepareRequest(httpGet);
	}
	
	public void post(String url) {
		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(this.prepareData(this.data));
		this.prepareRequest(httpPost);
	}
	
	public void put(String url) {
		HttpPut httpPut = new HttpPut(url);
		httpPut.setEntity(this.prepareData(this.data));
		this.prepareRequest(httpPut);
	}
	
	public void delete(String url) {
		HttpDelete httpDelete = new HttpDelete(url);
		this.prepareRequest(httpDelete);
	}
	
	protected HttpEntity prepareData(List<NameValuePair> nameValuePairs) {
        if (this.contentType != null && this.contentType.equalsIgnoreCase("application/json")) {
            JSONObject data = new JSONObject();
            try {
                for (int i=0; i<nameValuePairs.size(); i++) {
                    NameValuePair p = nameValuePairs.get(i);
                    data.put(p.getName(), p.getValue());
                }

                Log.e("RestRequestSending", data.toString());

                StringEntity se = new StringEntity(data.toString());
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                return se;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            try {
                return new UrlEncodedFormEntity(nameValuePairs);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }
	}
	
	private void prepareRequest(HttpRequest request) {
        if (this.contentType != null) {
            request.setHeader("Content-type", this.contentType);
        }

        request.setHeader("Accept", "application/json");

		this.asyncTask = new ExecuteAsyncRequest();
		this.asyncTask.execute(request);
	}
	
	public void cancelRequest() {
		if (this.asyncTask != null) {
			this.asyncTask.cancel(true);
		}
	}
	
	private Bundle executeRequest(HttpRequest request) {
        Bundle b = new Bundle();

        try {
            if (this.userAgent != null) {
                this.httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, this.userAgent);
	        }
	        
	        if (this.username != null && this.password != null) {
	            UsernamePasswordCredentials upc = new UsernamePasswordCredentials(this.username, this.password);
	            BasicScheme basicAuth = new BasicScheme();
	            request.addHeader(basicAuth.authenticate(upc, request));
	        }
            request.addHeader("Accept-Encoding", "gzip");
            
            HttpHost targetHost;
            if (this.useSsl == false) {
            	targetHost = new HttpHost(this.host, this.port, "http");
            } else {
            	targetHost = new HttpHost(this.host, this.sslPort, "https");
            }
            HttpResponse response = this.httpClient.execute(targetHost, request, this.requestContext);
 
            String responseData;
            HttpEntity entity = response.getEntity();
            Header contentEncoding = response.getFirstHeader("Content-Encoding");
            if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                InputStream instream = entity.getContent();
                instream = new GZIPInputStream(instream);
                
                InputStreamReader reader = new InputStreamReader(instream);
                BufferedReader in = new BufferedReader(reader);

                String readed;
                responseData = "";
                while ((readed = in.readLine()) != null) {
                	responseData += (readed);
                }
            } else {
            	responseData = EntityUtils.toString(entity);
            }
            
            if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300) {
            	b.putString("data", responseData);
            } else {
            	b.putString("statusCodeError", responseData);
                b.putInt("statusCodeErrorNumber", response.getStatusLine().getStatusCode());
            }
	    } catch (Exception e) {
	        e.printStackTrace();
	        b.putString("error", e.getMessage());
	    }
	    
	    return b;
	}
	
	private class ExecuteAsyncRequest extends AsyncTask<HttpRequest, Void, Bundle> {
		@Override
		protected Bundle doInBackground(HttpRequest... requests) {
			for (HttpRequest request : requests) {
				return RestRequest.this.executeRequest(request);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bundle b) {
		    Message m = new Message();
	        m.setData(b);
	        m.setTarget(RestRequest.this.handler);
	        m.sendToTarget();
		}
	}
	
	// taken from http://stackoverflow.com/questions/2642777/trusting-all-certificates-using-httpclient-over-https
	private DefaultHttpClient getNewHttpClient() {
		if (this.acceptAllSslCertificates == true) {
		    try {
		        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		        trustStore.load(null, null);
	
		        SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
		        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	
		        HttpParams params = new BasicHttpParams();
		        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
		        
		        // Set the timeout in milliseconds until a connection is established.
			    // The default value is zero, that means the timeout is not used. 
			    int timeoutConnection = 15000;
			    HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);
			    
			    // Set the default socket timeout (SO_TIMEOUT) 
			    // in milliseconds which is the timeout for waiting for data.
			    int timeoutSocket = 45000;
			    HttpConnectionParams.setSoTimeout(params, timeoutSocket);
	
			    SchemeRegistry registry = new SchemeRegistry();
		        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), this.port));
		        registry.register(new Scheme("https", sf, this.sslPort));
	
		        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
	
		        return new DefaultHttpClient(ccm, params);
		    } catch (Exception e) {
		        return new DefaultHttpClient();
		    }
		} else {
	        return new DefaultHttpClient();
		}
	}

	// taken from http://stackoverflow.com/questions/2642777/trusting-all-certificates-using-httpclient-over-https
	private class MySSLSocketFactory extends SSLSocketFactory {
	    SSLContext sslContext = SSLContext.getInstance("TLS");

	    public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
	        super(truststore);

	        TrustManager tm = new X509TrustManager() {
	            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	            }

	            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	            }

	            public X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	        };

	        this.sslContext.init(null, new TrustManager[] { tm }, null);
	    }

	    @Override
	    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
	        return this.sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
	    }

	    @Override
	    public Socket createSocket() throws IOException {
	        return this.sslContext.getSocketFactory().createSocket();
	    }
	}

}

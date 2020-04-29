package cn.olange.utils;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class HttpUtil {
	public static String postJson(String url, String data) throws IOException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		HttpPost httpPost = new HttpPost(url);
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(8000).setConnectTimeout(8000).build();
		httpPost.setConfig(requestConfig);
		StringEntity requestEntity = new StringEntity(data,"utf-8");
		httpPost.addHeader("Content-Type", "application/json;charset=utf-8");
		httpPost.addHeader("X-Agent", "Juejin/Web");
		httpPost.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36");
		httpPost.setEntity(requestEntity);
		return httpClient.execute(httpPost, responseHandler);
	}

	public static String getJson(String url) throws IOException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		HttpGet httpGet = new HttpGet(url);
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(8000).setConnectTimeout(8000).build();
		httpGet.setConfig(requestConfig);
		httpGet.addHeader("Content-Type", "application/json;charset=utf-8");
		httpGet.addHeader("X-Agent", "Juejin/Web");
		httpGet.addHeader("X-Juejin-Src","web");
		httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36");
		return httpClient.execute(httpGet, responseHandler);
	}
}

package ind.syu.restful;

import com.alibaba.fastjson.JSON;

import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import java.util.Map;

public class RestfulClient {

	public static Logger log = LoggerFactory.getLogger(RestfulClient.class);
	
	public enum Method { GET, POST ,PUT,DELETE}

	public static Map<String,Object> invokRestFul(String url, String requestJson,String head,Method httpMethod) {

		log.info("\n调用url:" + url+"\n调用报文:" + requestJson+"\n请求头:" + head);
		if(StringUtils.isEmpty(requestJson)){
			requestJson="{}";
		}
		CloseableHttpClient httpClient = HttpClients.createDefault();
		final HttpRequestBase httpRequest=getHttpMethod(httpMethod);
		httpRequest.setURI(URI.create(url));
		JSONObject request = JSON.parseObject(requestJson);
		JSONObject headJson = JSON.parseObject(head);
		StringEntity entity = new StringEntity(request.toString(), "utf-8");
		if(httpRequest instanceof HttpPost){
			((HttpPost) httpRequest).setEntity(entity);
		}
		if(httpRequest instanceof HttpPut){
			((HttpPut) httpRequest).setEntity(entity);
		}

		headJson.forEach((K, V) -> {
			httpRequest.addHeader(K, (String) V);
		});
		CloseableHttpResponse httppHttpResponse = null;
		try {
			httppHttpResponse = httpClient.execute(httpRequest);
		} catch (IOException e) {
			log.error("调用接口错误");
			log.error("\n调用url:" + url+"\n调用报文:" + requestJson+"\n请求头:" + head);
			log.error(e.getMessage());
		}
		int statusCode=httppHttpResponse.getStatusLine().getStatusCode();
		log.info("statusCode:"+statusCode);
		Map<String,Object> resultMap=new HashMap<>();
		HttpEntity result = null;
		resultMap.put("statusCode",statusCode);
		try {
			result = httppHttpResponse.getEntity();
			String s= EntityUtils.toString(result);
			Header heads[] = httppHttpResponse.getAllHeaders();
			Map<String,String> headMap=new HashMap<>();
			for(Header h:heads){
				headMap.put(h.getName(),h.getValue());
			}
			headMap.put("statusCode",String.valueOf(statusCode));
			headMap.put("url",url);
			resultMap.put("requestHead",head);
			resultMap.put("requestBody",requestJson);
			resultMap.put("responseHead",JSON.toJSON(headMap).toString());
			resultMap.put("responseBody",s);
			return resultMap;
		} catch (Exception e) {
			resultMap.put("result",e.getMessage());
			return resultMap;
		}finally{
			try {
				httppHttpResponse.close();
				httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		

	}
	
	private static HttpRequestBase getHttpMethod(Method method){
		switch(method){
			case POST:
				return new HttpPost();
			case GET:
				return new HttpGet();
			case PUT:
				return new HttpPut();
			case DELETE:
				return new HttpDelete();
			default:
				return null;
		}
	}



	public static Map<String, Object> invokRestFul(JsonResquestEntity en,Method httpMethod) {
		log.info("invokRestFul");
		JSONObject request = new JSONObject(en.getRequest());
		Map<String, Object> map = new HashMap<String, Object>();
		map.putAll(en.getHead());
		// en.getHead().forEach((K,V)->map.put(K, V));
		JSONObject head = new JSONObject(map);
		String url = en.getUrl();

		return invokRestFul(url, request.toString(), head.toString(),httpMethod);

	}




	

}

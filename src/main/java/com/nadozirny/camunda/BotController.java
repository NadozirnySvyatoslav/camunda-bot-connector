package com.nadozirny.camunda;
 
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;

org.apache.commons.configuration2.XMLConfiguration;

import java.util.regex.*;

import java.io.IOException;
 
@Path("/")

public class BotController 
{

    @POST
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response message(String data) throws IOException
    {
	JSONObject json=new JSONObject(data);
	String result="";
	JSONObject message=json.getJSONObject("message");
	JSONObject from=message.getJSONObject("from");
	JSONObject chat=message.getJSONObject("chat");
	int chat_id=chat.getInt("id");

	JSONObject correlationMessage=new JSONObject();
	correlationMessage.put("businessKey",chat_id);
	if (message!=null)
	    correlationMessage.put("processVariables",(new JSONObject()).put("message",(new JSONObject()).put("value",message)));

	String processInstanceId;

	CloseableHttpClient httpClient = HttpClients.createDefault();
	CloseableHttpResponse response=null;
	try{
	     HttpGet request = new HttpGet("http://localhost:8080/engine-rest/process-instance?processDefinitionKey="+process_key+"&businessKey="+chat_id);
	     response = httpClient.execute(request);
	    if (response.getStatusLine().getStatusCode()==200){
		 HttpEntity entity = response.getEntity();
                if (entity != null) {
		String entity_string=EntityUtils.toString(entity);
		    JSONArray json2=new JSONArray(entity_string);
		    if (json2.length()>0){
			JSONObject process=json2.getJSONObject(0);
			processInstanceId=process.getString("processInstanceId")
		    }
		}
	    }
	}finally{
	     httpClient.close();
	}

	if (processInstanceId){
	     HttpPost post = new HttpPost("http://localhost:8080/engine-rest/message");
	     post.addHeader("Content-Type", "application/json");
	     correlationMessage.put("messageName","update");
	     correlationMessage.put("processInstanceId",processInstanceId);
	     post.setEntity(new StringEntity(correlationMessage.toString(),"UTF-8"));
	     try{
	        httpClient = HttpClients.createDefault();
	        response = httpClient.execute(post);
    	     }finally{
	        httpClient.close();
	    }
	}
	else {
	     HttpPost post = new HttpPost("http://localhost:8080/engine-rest/message");
	     post.addHeader("Content-Type", "application/json");
	     correlationMessage.put("messageName","create");
	     post.setEntity(new StringEntity(correlationMessage.toString(),"UTF-8"));
	     try{
	        httpClient = HttpClients.createDefault();
	        response = httpClient.execute(post);
    	     }finally{
	        httpClient.close();
	    }

	}

    

    if (response!= null){
		if (response.getEntity()!=null && response.getEntity().getContentLength()>0){
		    result=EntityUtils.toString(response.getEntity());
		}else
		    result="ok\n";
	return Response.status(response.getStatusLine().getStatusCode()).entity(result).build();
    }else
	return Response.status(500).entity("Server error, no answer from Camunda server").build();
    }
}
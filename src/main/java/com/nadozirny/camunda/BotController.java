package com.nadozirny.camunda;
 
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;
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

import org.apache.commons.configuration2.XMLConfiguration ;

import java.util.regex.*;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
 
@Path("/")

public class BotController 
{
     private static final Logger LOGGER = LoggerFactory.getLogger(BotController.class);

    @POST
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response message(@QueryParam("token") String token,String data) throws IOException
    {
	HashMap<String,String> bots=Config.getBots();
	String process_key=bots.get(token);
	String camunda_engine=Config.config().getString("camunda_engine");
        LOGGER.debug("webhook bot: "+token+"  process: "+process_key);
	JSONObject json=new JSONObject(data);
	String result="";
	JSONObject message=json.getJSONObject("message");
	JSONObject from=message.getJSONObject("from");
	JSONObject chat=message.getJSONObject("chat");
	int chat_id=chat.getInt("id");
	int from_id=from.getInt("id");

	String business_key=chat_id+"_"+from_id;
	String processInstanceId="";

	CloseableHttpClient httpClient = HttpClients.createDefault();
	CloseableHttpResponse response=null;
	try{
	     HttpGet request = new HttpGet(camunda_engine+"/process-instance?processDefinitionKey="+process_key+"&businessKey="+business_key);
	     response = httpClient.execute(request);
	    if (response.getStatusLine().getStatusCode()==200){
		 HttpEntity entity = response.getEntity();
                if (entity != null) {
		String entity_string=EntityUtils.toString(entity);
		    JSONArray json2=new JSONArray(entity_string);
		    if (json2.length()>0){
			JSONObject process=json2.getJSONObject(0);
			processInstanceId=process.getString("id");
		    }
		}
	    }
	}finally{
	     httpClient.close();
	}

	if (processInstanceId != ""){
	     HttpPost post = new HttpPost(camunda_engine+"/message");
	     post.addHeader("Content-Type", "application/json");
	    JSONObject correlationMessage=new JSONObject();
	    correlationMessage.put("businessKey",business_key);
	    correlationMessage.put("messageName","update");
	    correlationMessage.put("processInstanceId",processInstanceId);

	    if (message!=null)
		correlationMessage.put("processVariables",(new JSONObject()).put("message",(new JSONObject()).put("value",message)));

            LOGGER.debug("update process {} ",correlationMessage);

	     post.setEntity(new StringEntity(correlationMessage.toString(),"UTF-8"));
	     try{
	        httpClient = HttpClients.createDefault();
	        response = httpClient.execute(post);
    	     }finally{
	        httpClient.close();
	    }
	}
	else {
	     HttpPost post = new HttpPost(camunda_engine+"/process-definition/key/"+process_key+"/start");
	     post.addHeader("Content-Type", "application/json");
	     JSONObject body=new JSONObject();
	     body.put("businessKey",business_key);
	     body.put("variables", (new JSONObject()).put("message",(new JSONObject()).put("value",message)));
             LOGGER.debug("start process {} ",body);
	     post.setEntity(new StringEntity(body.toString(),"UTF-8"));
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
	return Response.status(500).entity("{\"error\":\"Server error, no answer from Camunda server\"}").build();
    }
}
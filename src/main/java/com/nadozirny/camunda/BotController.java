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

import org.apache.commons.configuration2.XMLConfiguration;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;

import java.util.regex.*;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;

import java.util.regex.Matcher;  
import java.util.regex.Pattern;  
 
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
	if (token==null || token==""){
	    LOGGER.error("Token error {}",token);
	    return Response.status(404).entity("{\"error\":\"No token found\"}").build();
	}

	String process_key="";
	String business_key="";
	
        HashMap<String,String> bots=Config.getBots();

	String camunda_engine=Config.config().getString("camunda_engine");

	if (camunda_engine==null || camunda_engine==""){
	    LOGGER.error("Camunda url error camunda_engine= {}",camunda_engine);
	    return Response.status(404).entity("{\"error\":\"No camunda_engine found\"}").build();
	}

        LOGGER.debug("LOAD CONFIGURATION: url="+camunda_engine);
	LOGGER.debug("RECEIVED: {}",data);

	JSONObject json=new JSONObject(data);
	String result="";
	JSONObject message=null;
	JSONObject callback_query=null;
	if (json.has("message"))
	    message=json.getJSONObject("message");
    	if (json.has("callback_query"))
	    callback_query=json.getJSONObject("callback_query");

	if (message != null){
	    process_key=bots.get(token);	
	    JSONObject from=message.getJSONObject("from");
	    JSONObject chat=message.getJSONObject("chat");
    	    int chat_id=chat.getInt("id");
	    int from_id=from.getInt("id");
	    String hash="";
	    try{
	    MessageDigest digest = MessageDigest.getInstance("MD5");
	    byte[] bytes = digest.digest(((String)(chat_id+token+from_id)).getBytes("UTF-8"));
	    hash=DatatypeConverter.printHexBinary(bytes);
	    }catch(Exception e){
		business_key=chat_id+token.replace(':','_')+from_id;
	    }
	    business_key=hash;
	}

	if (callback_query != null){
	    Pattern  p=Pattern.compile("([a-zA-Z0-9_\\-]*):([a-zA-Z0-9_\\-]*):([a-zA-Z0-9_\\-]*)");
	    Matcher m=p.matcher(callback_query.getString("data"));
	    if (!m.matches()){
		LOGGER.error("No callback->data process_key:business_key:data found={}",callback_query.getString("data"));
		return Response.status(200).entity("{\"error\":\"No callback->data process_key:business_key:data found\"}").build();

	    }
	    process_key=m.group(1);
	    business_key=m.group(2);
	    json.put("data",m.group(3));
	}

	if (process_key==null || process_key==""){
	    LOGGER.error("Process error {}",token);
	    return Response.status(404).entity("{\"error\":\"No process found\"}").build();
	}

        LOGGER.debug("DATA : bot="+token+"  process="+process_key);

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

        if (message==null && (processInstanceId == null || processInstanceId == "" )){
		LOGGER.error("Cannot start process on callback");
		return Response.status(200).entity("{\"error\":\"Cannot start process on callback\"}").build();
	}

	if (processInstanceId != ""){
	     HttpPost post = new HttpPost(camunda_engine+"/message");
	     post.addHeader("Content-Type", "application/json");
	    JSONObject correlationMessage=new JSONObject();
	    correlationMessage.put("businessKey",business_key);
	    correlationMessage.put("messageName","update");
	    correlationMessage.put("processInstanceId",processInstanceId);

	    if (json!=null)
		correlationMessage.put("processVariables",(new JSONObject()).put("data",(new JSONObject()).put("value",json)));

            LOGGER.debug("UPDATE PROCESS: {} ",correlationMessage);

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
	     body.put("variables", (new JSONObject()).put("data",(new JSONObject()).put("value",json)));
             LOGGER.debug("START PROCESS: {} ",body);
	     post.setEntity(new StringEntity(body.toString(),"UTF-8"));
	     try{
	        httpClient = HttpClients.createDefault();
	        response = httpClient.execute(post);
    	     }finally{
	        httpClient.close();
	    }
	}

    if (response!= null){
        LOGGER.debug("RESPONSE: "+response.getStatusLine().getStatusCode()+" {}",response);
	result="{\"status\":\"ok\"}";
	return Response.status(response.getStatusLine().getStatusCode()).entity(result).build();
    }else{
        LOGGER.error("Server error, no answer from Camunda server {}",camunda_engine);
	return Response.status(500).entity("{\"error\":\"Server error, no answer from Camunda server\"}").build();
    }
    }
}
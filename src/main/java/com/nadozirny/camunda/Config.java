package com.nadozirny.camunda;

import org.apache.commons.configuration2.XMLConfiguration ;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Exception;

class Config{

    private static Config inst=new Config();
    XMLConfiguration config;

    public static HashMap<String,String> getBots(){
	if (inst==null) inst=new Config();
	HashMap<String,String> list=new HashMap<String,String>();
	int i=0;
	while(true){
	    if ((inst.config.getString("bots.bot("+i+").token") ==null) ||
		 (inst.config.getString("bots.bot("+i+").process_key")==null) ) break;

	    list.put(inst.config.getString("bots.bot("+i+").token"),
		inst.config.getString("bots.bot("+i+").process_key"));
	    i++;
	}
	return list;
    }
    public static XMLConfiguration config(){
	if (inst==null) inst=new Config();
	return inst.config;
    }
    private Config(){
	try{
	    Configurations configs = new Configurations();
	    config = configs.xml("configuration.xml");
	}
	catch(ConfigurationException e){
	    e.printStackTrace();
	}
    }

}
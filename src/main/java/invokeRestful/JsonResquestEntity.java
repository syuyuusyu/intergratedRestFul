package invokeRestful;

import java.util.Map;

public interface JsonResquestEntity {


	
	String getUrl();
	
	Map<String,Object> getRequest();
	
	Map<String,String> getHead();



}

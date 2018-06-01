package ind.syu.restful;


@FunctionalInterface
public interface InvokeCompleteEvent  {
	
	public  void exec(JsonResponseEntity data, final ThreadResultData resultData);

}

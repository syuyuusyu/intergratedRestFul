package ind.syu.restful;


public class CommonResponseEntity implements JsonResponseEntity{

    private String result;

    private int statusCode;

    private Class<?> clazz;



    @Override
    public void init(String jsonStr) {
        result=jsonStr;
    }



    @Override
    public Class<?> getResponseClass() {
        return clazz;
    }

    @Override
    public String getArrayJson() {
        return result;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }
}

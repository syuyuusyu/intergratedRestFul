package ind.syu.restful;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.springframework.util.StringUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestFulIntergrated {


    public  void invoke(List<InvokeEntity> ens,final ThreadResultData trd){
        ens.forEach(e->{invoke(e,trd);});
    }

    public void invoke(InvokeEntity en,final ThreadResultData trd){


        InvokeUniversal invoke=new InvokeUniversal(en.getName());
        invoke
                .setUrl(en.parseUrl())
                .setMethod(en.getMethod())
                .setRequestBody(en.parseBody())
                .setRequstHead(en.parseHead());
        if(en.transferMap()!=null){
            en.transferMap().forEach(invoke::putTransferMap);
        }
        if(en.invokeCompleteEvent()!=null){
            invoke.addEvent(en.invokeCompleteEvent());
        }
        if(!StringUtils.isEmpty(en.getParseFun())){
            invoke.setBiResultFun((resopne,result)->{
                long time=System.currentTimeMillis();
                //System.out.println("result = \n" + result);
                String function=en.getParseFun();
                String funName=getFunName(function);
                ScriptObjectMirror res=null;

                try {
                    ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");

                    engine.eval("var responseBody=" + result);
                    engine.eval("var requestHead=" + resopne.get("requestHead"));

                    engine.eval("var requestBody=" + resopne.get("requestBody"));
                    engine.eval("var responseHead=" + resopne.get("responseHead"));

                    Object responseBody = engine.get("responseBody");
                    Object requestHead = engine.get("requestHead");
                    Object requestBody = engine.get("requestBody");
                    Object responseHead = engine.get("responseHead");

                    engine.eval(function);
                    Invocable inv = (Invocable) engine;
                    //response,responsehead,responsestatus,requesthead,requestdata,url
                    res = (ScriptObjectMirror) inv.invokeFunction(funName, responseBody,responseHead,requestHead,requestBody);

                } catch (ScriptException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                JSON json=null;
                if(res.isArray()){
                    json=parseArr(res);
                }else{
                    json=parseObj(res);
                }
                System.out.println("time:"+ (System.currentTimeMillis()-time));
                return json.toJSONString();
            });
        }

        if(en.next()!=null && en.next().size()>0){
            invoke.addEvent((data,trdd)->{
                String result=data.getArrayJson();
                boolean isArray=true;
                JSONArray arrJson=null;
                try {
                    arrJson=JSON.parseArray(result);
                }catch (Exception e){
                    isArray=false;
                }

                for(int i=0;i< (isArray?arrJson.size():1);i++){
                    JSONObject json= isArray?arrJson.getJSONObject(i):JSON.parseObject(result);
                    en.next().stream().map(e->{
                        List<String> params=e.queryParams();
                        Map<String,String> queryMap=e.getQueryMap()!=null?e.getQueryMap():new HashMap<>();

                        params.stream().filter(p->!queryMap.containsKey(p)).forEach(p->{
                            String value=json.getString(p);
                            if(StringUtils.isEmpty(value)){
                                if(en.getQueryMap().containsKey(p)){
                                    value=en.getQueryMap().get(p);
                                }else{
                                    System.err.println("调用参数:"+p+" 找不到对应值");
                                }
                            }
                            queryMap.put(p,value);
                        });
                        e.setQueryMap(queryMap);
                        return e;
                    }).forEach(e->{
                        invoke(e,trdd);
                    });
                }
            });
        }
        trd.addInvoker(invoke);

    }

    public  String applyFun(String function,String result){
        String funName=getFunName(function);
        ScriptObjectMirror res=null;

        try {
            ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");
            engine.eval("var result=" + result);
            Object responseBody = engine.get("result");

            engine.eval(function);
            Invocable inv = (Invocable) engine;
            res = (ScriptObjectMirror) inv.invokeFunction(funName, responseBody);

        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        JSON json=null;
        if(res.isArray()){
            json=parseArr(res);
        }else{
            json=parseObj(res);
        }
        return json.toJSONString();
    }

    private String getFunName(String fun){
        Matcher m= Pattern.compile("function\\s+(\\w+)\\s?\\(\\s?\\w+\\s?\\)").matcher(fun);
        String funName="parse";
        while(m.find()){
            funName=m.group(1);
        }
        return funName;
    }


    private JSONArray parseArr(ScriptObjectMirror som){
        JSONArray jarr=new JSONArray();
        som.keySet().forEach((String key) ->{
            Object each=som.get(key);
            switch (each.getClass().getName()){
                case "jdk.nashorn.api.scripting.ScriptObjectMirror":
                    ScriptObjectMirror om=(ScriptObjectMirror)each;
                    if(om.isArray())
                        jarr.add(Integer.valueOf(key),parseArr(om));
                    else
                        jarr.add(Integer.valueOf(key),parseObj(om));
                    break;
                case "java.lang.String":
                    jarr.add(Integer.valueOf(key),(String)each);
                    break;
                case "java.lang.Integer":
                    jarr.add(Integer.valueOf(key),(Integer)each);
                    break;
                case "java.lang.Double":
                    jarr.add(Integer.valueOf(key),(Double)each);
                    break;
            }
        });
        return jarr;
    }

    private JSONObject parseObj(ScriptObjectMirror som){
        JSONObject obj=new JSONObject();
        som.keySet().forEach((String key) ->{
            Object each=som.get(key);
            if(each==null){
                obj.put(key,"");
            }else {
                switch (each.getClass().getName()) {
                    case "jdk.nashorn.api.scripting.ScriptObjectMirror":
                        ScriptObjectMirror om = (ScriptObjectMirror) each;
                        if (om.isArray())
                            obj.put(key, parseArr(om));
                        else
                            obj.put(key, parseObj(om));
                        break;
                    case "java.lang.String":
                        obj.put(key, (String) each);
                        break;
                    case "java.lang.Integer":
                        obj.put(key, (Integer) each);
                        break;
                    case "java.lang.Double":
                        obj.put(key, (Double) each);
                        break;
                }
            }

        });
        return obj;
    }
}

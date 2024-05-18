package com.dingding.mid.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.dingding.mid.dto.json.ChildNode;
import com.dingding.mid.dto.json.Properties;
import com.dingding.mid.dto.json.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.de.odysseus.el.ExpressionFactoryImpl;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.dingding.mid.common.CommonConstants.START_USER_INFO;
import static com.dingding.mid.common.WorkFlowConstants.*;
import static com.dingding.mid.utils.BpmnModelUtils.getChildNode;

/**
 * @author LoveMyOrange
 * @create 2022-10-16 22:13
 */
@Component
@Slf4j
public class ExUtils {
    public Boolean strEqualsMethod(String controlId,String value){
        List<String> list = Arrays.asList(value);
            String s = list.get(0);
            log.debug("进行字符串相等判断,用户填的值{},设计器输入的值{}",controlId,s);
            return s.equals(controlId);
    }
    public Boolean strEqualsMethod(String controlId,String...values){
        List<String> list = Arrays.asList(values);
        if(list.size()>1){
            log.debug("进行字符串相等判断,用户填的值{},设计器输入的值{}",controlId,list);
            return Boolean.FALSE;
        }
        else{
            String s = list.get(0);
            log.debug("进行字符串相等判断,用户填的值{},设计器输入的值{}",controlId,s);
            return s.equals(controlId);
        }
    }

    public Boolean strContains(String controlId,String...values){
        List<String> list = Arrays.asList(values);
        log.debug("进行字符串包含判断,用户填的值{},设计器输入的值{}",controlId,list);
        return list.contains(controlId);
    }

    public Boolean strContains(String controlId,Number...values){
        Long aLong = Long.valueOf(controlId);
        List<Number> numbers = Arrays.asList(values);
        return numbers.contains(aLong);
    }

    public Boolean strContainsMethod(String controlId,String...values){
        List<String> strings = Arrays.asList(values);
        log.debug("进行字符串包含判断,用户填的值{},设计器输入的值{}",controlId,strings);
        return strings.contains(controlId);
    }


    public Boolean userStrContainsMethod(String controlId, String fromText, DelegateExecution execution){
        Object executionVariable = execution.getVariable(controlId);
        if(executionVariable instanceof String){
            String variable = (String) execution.getVariable(controlId);
            if(StringUtils.isBlank(variable)){
                return Boolean.FALSE;
            }
            List<UserInfo> userInfos = JSONObject.parseObject(variable, new TypeReference<List<UserInfo>>() {
            });
            List<String> idsList= new ArrayList<>();
            for (UserInfo userInfo : userInfos) {
                idsList.add(userInfo.getId());
            }
            String[] split = fromText.split(",");
            List<String> strings = Arrays.asList(split);
            Collection<String> intersection = CollUtil.intersection(strings, idsList);
            if(CollUtil.isEmpty(intersection)){
                return Boolean.FALSE;
            }
            return Boolean.TRUE;

        }
        else{
            JSONArray variable = (JSONArray) execution.getVariable(controlId);
            List<UserInfo> userInfos = JSONObject.parseObject(variable.toJSONString(), new TypeReference<List<UserInfo>>() {
            });
            List<String> idsList= new ArrayList<>();
            for (UserInfo userInfo : userInfos) {
                idsList.add(userInfo.getId());
            }
            String[] split = fromText.split(",");
            List<String> strings = Arrays.asList(split);
            Collection<String> intersection = CollUtil.intersection(strings, idsList);
            if(CollUtil.isEmpty(intersection)){
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

    }

    public Boolean userStrContainsMethod(String controlId,String...values){
        List<String> strings = Arrays.asList(values);
        List<UserInfo> userInfos = JSONObject.parseObject(controlId, new TypeReference<List<UserInfo>>() {
        });
        List<String> idsList= new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            idsList.add(userInfo.getId());
        }
        Collection<String> intersection = CollUtil.intersection(strings, idsList);
        if(CollUtil.isEmpty(intersection)){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public Boolean deptStrContainsMethod(String controlId,String fromText,DelegateExecution execution){
        Object executionVariable = execution.getVariable(controlId);
        if(executionVariable instanceof String){
            String variable = (String) execution.getVariable(controlId);
            if(StringUtils.isBlank(variable)){
                return Boolean.FALSE;
            }
            List<UserInfo> userInfos = JSONObject.parseObject(variable, new TypeReference<List<UserInfo>>() {
            });
            List<String> idsList= new ArrayList<>();
            for (UserInfo userInfo : userInfos) {
                idsList.add(userInfo.getId());
            }
            String[] split = fromText.split(",");
            List<String> strings = Arrays.asList(split);
            Collection<String> intersection = CollUtil.intersection(strings, idsList);
            if(CollUtil.isEmpty(intersection)){
                return Boolean.FALSE;
            }
            return Boolean.TRUE;

        }
        else{
            JSONArray variable = (JSONArray) execution.getVariable(controlId);
            List<UserInfo> userInfos = JSONObject.parseObject(variable.toJSONString(), new TypeReference<List<UserInfo>>() {
            });
            List<String> idsList= new ArrayList<>();
            for (UserInfo userInfo : userInfos) {
                idsList.add(userInfo.getId());
            }
            String[] split = fromText.split(",");
            List<String> strings = Arrays.asList(split);
            Collection<String> intersection = CollUtil.intersection(strings, idsList);
            if(CollUtil.isEmpty(intersection)){
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

    }



    public Boolean numberContains(Number controlId,Number...values){
        List<Number> list = Arrays.asList(values);
        return list.contains(controlId);
    }
    public Boolean b(String controlId,Number...values){
        List<Number> numbers = Arrays.asList(values);

        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(numbers.get(0).doubleValue());
        BigDecimal a2 = BigDecimal.valueOf(b);

        Double c = Double.valueOf(numbers.get(1).doubleValue());
        BigDecimal a3 = BigDecimal.valueOf(c);


        if( NumberUtil.isGreater(a1,a2)  && NumberUtil.isLess(a1,a3)  ){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }



    public Boolean ab(String controlId,Number...values){
        List<Number> numbers = Arrays.asList(values);

        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(numbers.get(0).doubleValue());
        BigDecimal a2 = BigDecimal.valueOf(b);

        Double c = Double.valueOf(numbers.get(1).doubleValue());
        BigDecimal a3 = BigDecimal.valueOf(c);


        if(NumberUtil.isGreaterOrEqual(a1,a2)  &&NumberUtil.isLess(a1,a3)){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
    public Boolean ba(String controlId,Number...values){

        List<Number> numbers = Arrays.asList(values);

        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(numbers.get(0).doubleValue());
        BigDecimal a2 = BigDecimal.valueOf(b);

        Double c = Double.valueOf(numbers.get(1).doubleValue());
        BigDecimal a3 = BigDecimal.valueOf(c);


        if(NumberUtil.isGreater(a1,a2)  &&NumberUtil.isLessOrEqual(a1,a3)){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;


    }
    public Boolean aba(String controlId,Number...values){
        List<Number> numbers = Arrays.asList(values);

        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(numbers.get(0).doubleValue());
        BigDecimal a2 = BigDecimal.valueOf(b);

        Double c = Double.valueOf(numbers.get(1).doubleValue());
        BigDecimal a3 = BigDecimal.valueOf(c);


        if(NumberUtil.isGreaterOrEqual(a1,a2)  &&NumberUtil.isLessOrEqual(a1,a3)){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


    /**
     conditionExpression.append(" "+ EXPRESSION_CLASS+"numberEquals("+id+","+str+") " );
     conditionExpression.append(" "+ EXPRESSION_CLASS+"numberGt("+id+","+str+") " );
     conditionExpression.append(" "+ EXPRESSION_CLASS+"numberGtEquals("+id+","+str+") " );
     conditionExpression.append(" "+ EXPRESSION_CLASS+"numberLt("+id+","+str+") " );
     conditionExpression.append(" "+ EXPRESSION_CLASS+"numberLtEquals("+id+","+str+") " );
     */
    public Boolean numberEquals(String controlId,String value){
        Double a = Double.valueOf(controlId);
        Double b = Double.valueOf(value);
        boolean equals = a.equals(b);
        return equals;
    }
    public Boolean numberGt(String controlId,String value){
        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(value);
        BigDecimal a2 = BigDecimal.valueOf(b);
        boolean greater = NumberUtil.isGreater(a1, a2);
        return greater;
    }

    public Boolean numberGtEquals(String controlId,String value){
        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(value);
        BigDecimal a2 = BigDecimal.valueOf(b);
        boolean greater = NumberUtil.isGreaterOrEqual(a1, a2);
        return greater;
    }

    public Boolean numberLt(String controlId,String value){
        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(value);
        BigDecimal a2 = BigDecimal.valueOf(b);
        boolean greater = NumberUtil.isLess(a1, a2);
        return greater;
    }
    public Boolean numberLtEquals(String controlId,String value){
        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(value);
        BigDecimal a2 = BigDecimal.valueOf(b);
        boolean greater = NumberUtil.isLessOrEqual(a1, a2);
        return greater;
    }
    public String mailContent(DelegateExecution execution,String mailContent){
        byte[] decode = Base64.decode(mailContent.getBytes(Charset.defaultCharset()));
        mailContent=new String(decode);
        Map<String, Object> variables = execution.getVariables();
        Map<String,Object> finalVariables = new HashMap<>();
        finalVariables.putAll(variables);

        RepositoryService repositoryService = SpringContextHolder.getBean(RepositoryService.class);
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(execution.getProcessDefinitionId()).singleResult();
        finalVariables.put("formName",processDefinition.getName());
        Map<String, Object> processVariables = execution.getVariables();
        UserInfo userInfo = JSONObject.parseObject(MapUtil.getStr(processVariables, START_USER_INFO), new TypeReference<UserInfo>() {
        });
        finalVariables.put("ownerId",userInfo.getId());
        finalVariables.put("ownerName",userInfo.getName());
        finalVariables.put("ownerDeptId","部门id请自己维护在各自的User对象中");
        finalVariables.put("ownerDeptName","部门Name请自己维护在各自的User对象中");
        finalVariables.put("instanceId",execution.getProcessInstanceId());


        ExpressionFactory factory = new ExpressionFactoryImpl();
        SimpleContext context = new SimpleContext();
        Set<String> strings = finalVariables.keySet();
        for (String string : strings) {
            context.setVariable(string,factory.createValueExpression(finalVariables.get(string),Object.class));
        }
        ValueExpression valueExpression = factory.createValueExpression(context, mailContent, String.class);
        Object value = valueExpression.getValue(context);
        return (String) value;
    }
    public String requestBody(DelegateExecution execution,String bodyStr){
        byte[] decode = Base64.decode(bodyStr.getBytes(Charset.defaultCharset()));
        bodyStr=new String(decode);
        List<Map> list = JSONObject.parseObject(bodyStr, new TypeReference<List<Map>>() {
        });
        Map<String,Object> bodyMap = new HashMap<>();
        for (Map map : list) {
            Boolean isField = MapUtil.getBool(map, "isField");
            String name = MapUtil.getStr(map, "name");
            String value =MapUtil.getStr(map,"value");
            if(isField){
                String s = execution.getVariable(value).toString();
                bodyMap.put(name,s);
            }
            else{
                Map<String, Object> processVariables = execution.getVariables();
                UserInfo userInfo = JSONObject.parseObject(MapUtil.getStr(processVariables, START_USER_INFO), new TypeReference<UserInfo>() {
                });
                //进行特殊条件判断
                RepositoryService repositoryService = SpringContextHolder.getBean(RepositoryService.class);
                ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(execution.getProcessDefinitionId()).singleResult();
                if("formName".equals(value)){
                    bodyMap.put(name,processDefinition.getName());
                }
                else if ("ownerId".equals(value)) {
                    bodyMap.put(name,userInfo.getId());
                }
                else if ("ownerName".equals(value)) {
                    bodyMap.put(name,userInfo.getName());
                }
                else if ("ownerDeptId".equals(value)) {
                    bodyMap.put(name,"部门id请自己维护在各自的User对象中");
                }
                else if ("ownerDeptName".equals(value)) {
                    bodyMap.put(name,"部门name请自己维护在各自的User对象中");
                }
                else if ("instanceId".equals(value)) {
                    bodyMap.put(name,execution.getProcessInstanceId());
                }
                else{
                    bodyMap.put(name,value);
                }

            }
        }

        return JSONObject.toJSONString(bodyMap);
    }

    /**
     *
     * @param execution
     * @return
     */
    public String timeDate(DelegateExecution execution){
        ChildNode childNode = getNode(execution);
        ChildNode node = getChildNode(childNode, execution.getCurrentActivityId());
        Properties props = node.getProps();
        String dateTime = props.getDateTime();
        String[] split1 = dateTime.split(":");
        //
        Date date=new Date();
        date.setHours(Integer.valueOf(split1[0]));
        date.setMinutes(Integer.valueOf(split1[1]));
        date.setSeconds(Integer.valueOf(split1[2]));
        String format = DateUtil.format(date, "yyyy-MM-dd'T'HH:mm:ssXXX");
        return format;
    }

    private ChildNode getNode(DelegateExecution execution) {
        RepositoryService repositoryService = SpringContextHolder.getBean(RepositoryService.class);
        Process mainProcess = repositoryService.getBpmnModel(execution.getProcessDefinitionId()).getMainProcess();
        String dingDing = mainProcess.getAttributeValue(FLOWABLE_NAME_SPACE, FLOWABLE_NAME_SPACE_NAME);
        JSONObject jsonObject = JSONObject.parseObject(dingDing, new TypeReference<JSONObject>() {
        });
        String processJson = jsonObject.getString(VIEW_PROCESS_JSON_NAME);
        ChildNode childNode = JSONObject.parseObject(processJson, new TypeReference<ChildNode>(){});
        return childNode;
    }

    public String requestHeaders(DelegateExecution execution,String headerStr){
        byte[] decode = Base64.decode(headerStr.getBytes(Charset.defaultCharset()));
        headerStr=new String(decode);
        List<Map> list = JSONObject.parseObject(headerStr, new TypeReference<List<Map>>() {
        });
        String headerResultStr="";
        for (Map map : list) {
            Boolean isField = MapUtil.getBool(map, "isField");
            String name = MapUtil.getStr(map, "name");
            String value = MapUtil.getStr(map, "value");
            headerResultStr+=(name+": ");
            if(isField){
                String s = execution.getVariable(value).toString();
                headerResultStr+=s+" \n";
            }
            else{
                headerResultStr+=value+" \n";
            }
        }
        return headerResultStr;
    }
}

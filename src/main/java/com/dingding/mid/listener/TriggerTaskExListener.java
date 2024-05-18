package com.dingding.mid.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.dingding.mid.dto.json.ChildNode;
import com.dingding.mid.dto.json.HttpInfo;
import com.dingding.mid.dto.json.UserInfo;
import com.dingding.mid.exception.WorkFlowException;
import com.dingding.mid.utils.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dingding.mid.common.CommonConstants.START_USER_INFO;
import static com.dingding.mid.common.WorkFlowConstants.*;
import static com.dingding.mid.utils.BpmnModelUtils.getChildNode;

/**
 * @author Doctor4JavaEE
 * @since 2024/3/26
 */
@Component
@Slf4j
public class TriggerTaskExListener implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        ChildNode childNode = getNode(execution);
        ChildNode node = getChildNode(childNode, execution.getCurrentActivityId());
        HttpInfo http = node.getProps().getHttp();
        String method = http.getMethod();
        String url = http.getUrl();
        HttpRequest httpRequest=null;
        if("GET".equals(method)){
            httpRequest=HttpRequest.get(url);
        }
        else if("POST".equals(method)){
            httpRequest=HttpRequest.post(url);
        }
        else if("DELETE".equals(method)){
            httpRequest=HttpRequest.delete(url);
        }
        else if("PUT".equals(method)){
            httpRequest=HttpRequest.put(url);
        }
        else{
            throw new WorkFlowException("哪有??");
        }
        if(httpRequest!=null){
            //1.0 对http本身进行个性化设置
            httpRequest.setConnectionTimeout(100000000);//永不超时,一直等
            httpRequest.setReadTimeout(100000000);//永不超时,一直等
            //1.1 处理header ,不支持占位, 如果需要可以增加
            List<Map<String, Object>> headers = http.getHeaders();
            if(CollUtil.isNotEmpty(headers)){
                for (Map<String, Object> header : headers) {
                    Boolean isField = MapUtil.getBool(header, "isField");
                    String name = MapUtil.getStr(header, "name");
                    String value = MapUtil.getStr(header, "value");
                    if(isField){
                        Object variable = execution.getVariable(value);
                        if(variable!=null){
                            httpRequest.header(name,variable.toString());
                        }
                    }
                    else{
                        httpRequest.header(name,value);
                    }
                }
            }
            //1.2处理 body, 支持占位, 可以增加更多占位
            String contentType = http.getContentType();
            List<Map<String, Object>> params = http.getParams();
            //1.3两种类型, 一种表单提交 , 一种application/json
            if(CollUtil.isNotEmpty(params)){
                Map<String,Object> bodyMap = new HashMap<>();
                for (Map map : params) {
                    Boolean isField = MapUtil.getBool(map, "isField");
                    String name = MapUtil.getStr(map, "name");
                    String value =MapUtil.getStr(map,"value");
                    if(isField){
                        Object variable = execution.getVariable(value);
                        bodyMap.put(name,variable);
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
                if("JSON".equals(contentType)){
                    httpRequest.body(JSONObject.toJSONString(bodyMap),"application/json");
                }
                else{
                    httpRequest.form(bodyMap);
                }
            }
            //1.4 执行
            HttpResponse execute = httpRequest.execute();
            //1.5个性化处理, 摒弃HttpTask的原因在这
            if(execute.isOk()){
                log.info("该请求正确执行,即将流转!!!");
            }
            else{
                String body = execute.body();
                throw new WorkFlowException("Http Task请求报错!!!!!!!!失败原因是"+body);
            }


        }


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
}

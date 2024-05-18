package com.dingding.mid.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dingding.mid.dto.json.*;
import com.dingding.mid.dto.json.Properties;
import com.dingding.mid.entity.Users;
import com.dingding.mid.enums.AssigneeTypeEnums;
import com.dingding.mid.enums.ModeEnums;
import com.dingding.mid.exception.WorkFlowException;
import com.dingding.mid.service.UserService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.spring.integration.Flowable;
import org.springframework.util.CollectionUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static com.dingding.mid.common.WorkFlowConstants.*;
import static org.flowable.bpmn.model.ImplementationType.IMPLEMENTATION_TYPE_CLASS;
import static org.flowable.bpmn.model.ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION;

/**
 * @author LoveMyOrange
 * @create 2022-10-10 17:47
 */
public class    BpmnModelUtils {

    private static String id(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    private static ServiceTask serviceTask(String name) {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setName(name);
        return serviceTask;
    }

    public static SequenceFlow connect(String from, String to,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap,Process process) {
        SequenceFlow flow = new SequenceFlow();
        String  sequenceFlowId = id("sequenceFlow");
        if(process.getFlowElement(from) !=null && ((process.getFlowElement(from) instanceof ExclusiveGateway) || (process.getFlowElement(from) instanceof InclusiveGateway))){
            ChildNode childNode = childNodeMap.get(to);
            if(childNode!=null){
                String parentId = childNode.getParentId();
                if(StringUtils.isNotBlank(parentId)){
                    ChildNode parentNode = childNodeMap.get(parentId);
                    if(parentNode!=null){
                        if(Type.CONDITION.type.equals(parentNode.getType())  || Type.INCLUSIVE.type.equals(parentNode.getType()) ){
                            sequenceFlowId=parentNode.getId();
                            flow.setName(parentNode.getName());

                            if(Boolean.FALSE.equals(parentNode.getTypeElse())){
                                //解析条件表达式
                                Properties props = parentNode.getProps();
                                String expression = props.getExpression();
                                List<GroupsInfo> groups = props.getGroups();
                                String groupsType = props.getGroupsType();
                                if(StringUtils.isNotBlank(expression)){
                                    flow.setConditionExpression("${"+expression+"}");
                                }
                                else {

                                    StringBuffer conditionExpression=new StringBuffer();
                                    conditionExpression.append("${ ");
                                    //精髓代码实现4 拼装条件表达式, 我们使用JUEL METHOD 方法, 这种方式便于理解,flowable自身通过反射 Invoke 到 ExUtils类 执行对应方法, 有问题或者要新增逻辑  可以直接按照下方代码新增
                                    for (int i = 0; i < groups.size(); i++) {
                                        conditionExpression.append(" ( ");
                                        GroupsInfo group = groups.get(i);
                                        List<String> cids = group.getCids();
                                        String groupType = group.getGroupType();
                                        List<ConditionInfo> conditions = group.getConditions();
                                        for (int j = 0; j < conditions.size(); j++) {
                                            conditionExpression.append(" ");
                                            ConditionInfo condition = conditions.get(j);
                                            String compare = condition.getCompare();
                                            String id = condition.getId();
                                            String title = condition.getTitle();
                                            List<Object> value = condition.getValue();
                                            String valueType = condition.getValueType();
                                            if("String".equals(valueType)){
                                                if("=".equals(compare)){
                                                    String str = StringUtils.join(value, ",");
                                                    str="'"+str+"'";
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"strEqualsMethod("+id+","+str+") " );
                                                }
                                                else{
                                                    List<String> tempList=new ArrayList<>();
                                                    for (Object o : value) {
                                                        String s = o.toString();
                                                        s="'"+s+"'";
                                                        tempList.add(s);
                                                    }
                                                    String str = StringUtils.join(tempList, ",");
//                                                String str = StringUtils.join(value, ",");
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"strContainsMethod("+id+","+str+") " );
                                                }
                                            }
                                            else if("Number".equals(valueType)){
                                                String str = StringUtils.join(value, ",");
                                                if("=".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberEquals("+id+","+str+") " );
                                                }
                                                else if(">".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberGt("+id+","+str+") " );
                                                }
                                                else if(">=".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberGtEquals("+id+","+str+") " );
                                                }
                                                else if("<".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberLt("+id+","+str+") " );
                                                }
                                                else if("<=".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberLtEquals("+id+","+str+") " );
                                                }
                                                else if("IN".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberContains("+id+","+str+") " );
                                                }
                                                else if("B".equals(compare)){
                                                    conditionExpression.append("  "+ EXPRESSION_CLASS+"b("+id+","+str+") " );
                                                }
                                                else if("AB".equals(compare)){
                                                    conditionExpression.append("  "+ EXPRESSION_CLASS+"ab("+id+","+str+") " );
                                                }
                                                else if("BA".equals(compare)){
                                                    conditionExpression.append("  "+ EXPRESSION_CLASS+"ba("+id+","+str+") " );
                                                }
                                                else if("ABA".equals(compare)){
                                                    conditionExpression.append("  "+ EXPRESSION_CLASS+"aba("+id+","+str+") " );
                                                }
                                            }
                                            else if("User".equals(valueType)){
                                                List<String> userIds=new ArrayList<>();
                                                for (Object o : value) {
                                                    JSONObject obj=(JSONObject)o;
                                                    userIds.add(obj.getString("id"));
                                                }
                                                String str=" "+ EXPRESSION_CLASS+"userStrContainsMethod(\"{0}\",\"{1}\",    execution) ";
                                                str = str.replace("{0}", id);
                                                str = str.replace("{1}", StringUtils.join(userIds, ","));
                                                conditionExpression.append(str );
                                            }
                                            else if("Dept".equals(valueType)){
                                                List<String> userIds=new ArrayList<>();
                                                for (Object o : value) {
                                                    JSONObject obj=(JSONObject)o;
                                                    userIds.add(obj.getString("id"));
                                                }
                                                String str=" "+ EXPRESSION_CLASS+"deptStrContainsMethod(\"{0}\",\"{1}\",    execution) ";
                                                str = str.replace("{0}", id);
                                                str = str.replace("{1}", StringUtils.join(userIds, ","));
                                                conditionExpression.append(str );
                                            }
                                            else{
                                                continue;
                                            }

                                            if(conditions.size()>1 && j!=(conditions.size()-1)){
                                                if("OR".equals(groupType)){
                                                    conditionExpression.append(" || ");
                                                }
                                                else {
                                                    conditionExpression.append(" && ");
                                                }
                                            }

                                            if(i==(conditions.size()-1)){
                                                conditionExpression.append(" ");
                                            }
                                        }


                                        conditionExpression.append(" ) ");

                                        if(groups.size()>1 && i!=(groups.size()-1) ){
                                            if("OR".equals(groupsType)){
                                                conditionExpression.append(" || ");
                                            }
                                            else {
                                                conditionExpression.append(" && ");
                                            }
                                        }


                                    }
                                    conditionExpression.append("} ");
                                    flow.setConditionExpression(conditionExpression.toString());
                                }
                            }
                        }
                    }
                }
            }
        }
        flow.setId(sequenceFlowId);
        flow.setSourceRef(from);
        flow.setTargetRef(to);
        sequenceFlows.add(flow);
        return flow;
    }

    private static String stringEquals(ConditionInfo condition) {
        return null;
    }


    public static StartEvent createStartEvent() {
        StartEvent startEvent = new StartEvent();
        startEvent.setId(START_EVENT_ID);
        startEvent.setInitiator("applyUserId");
        return startEvent;
    }

    public static EndEvent createEndEvent() {
        EndEvent endEvent = new EndEvent();
        endEvent.setId(END_EVENT_ID);
        return endEvent;
    }

    //精髓代码实现3 如何进行的JSON-BPMN完整逻辑组装
    public static String create(String fromId, ChildNode flowNode, Process process,BpmnModel bpmnModel,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        String nodeType = flowNode.getType();
        if (Type.CONCURRENTS.isEqual(nodeType)) {
            return createParallelGatewayBuilder(fromId, flowNode,process,bpmnModel,sequenceFlows,childNodeMap);
        } else if (Type.CONDITIONS.isEqual(nodeType)) {
            return createExclusiveGatewayBuilder(fromId, flowNode,process,bpmnModel,sequenceFlows,childNodeMap);
        }
        //包容网关 后端做了前端没做, 和排他网关一模一样,前端只更换一个type既可
        else if (Type.IN_CONDITIONS.isEqual(nodeType)) {
            return createInclusiveGatewayBuilder(fromId, flowNode,process,bpmnModel,sequenceFlows,childNodeMap);
        }

        else if (Type.USER_TASK.isEqual(nodeType)) {
            childNodeMap.put(flowNode.getId(),flowNode);
            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));
            String id = createTask(process,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
            } else {
                return id;
            }
        }
        //办理人节点 后端做了前端没做, 和审批节点一模一样,前端只更换一个type既可
        else if (Type.APPROVE_USER_TASK.isEqual(nodeType)) {
            childNodeMap.put(flowNode.getId(),flowNode);
            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));
            String id = createTask(process,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
            } else {
                return id;
            }
        }
        else if(Type.ROOT.isEqual(nodeType)){
            childNodeMap.put(flowNode.getId(),flowNode);
            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));
            String id = createTask(process,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
            } else {
                return id;
            }
        }
        else if(Type.DELAY.isEqual(nodeType)){
            childNodeMap.put(flowNode.getId(),flowNode);
            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));
            String id = createDelayTask(process,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
            } else {
                return id;
            }
        }
        else if(Type.TRIGGER.isEqual(nodeType)){
            childNodeMap.put(flowNode.getId(),flowNode);
            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));
            //旧版使用 HttpTask实现 ,诸多不灵活, 我们使用servcieeeTask实现 ,
            // 保留以前旧版代码,供大家学习茴香豆的茴有几种写法. 我就是天才 哈哈哈
//            String id = createTriggerTask(process,flowNode,sequenceFlows,childNodeMap);
            String id=createNewTriggerTask(process,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
            } else {
                return id;
            }
        }
        else if(Type.CC.isEqual(nodeType)){
            childNodeMap.put(flowNode.getId(),flowNode);
            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));
            String id = createServiceTask(process,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
            } else {
                return id;
            }
        }
        else if(Type.SUBPROCESS.type.equals(nodeType)){
            childNodeMap.put(flowNode.getId(),flowNode);
            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));
            String id = createCallActivity(process,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
            } else {
                return id;
            }
        }
        else{
            throw new WorkFlowException("哪有？？");
        }
    }

    private static String createNewTriggerTask(Process process, ChildNode flowNode, List<SequenceFlow> sequenceFlows, Map<String, ChildNode> childNodeMap) {
        JSONObject incomingJson = flowNode.getIncoming();
        List<String> incoming = incomingJson.getJSONArray("incoming").toJavaList(String.class);
        // 自动生成id
//        String id = id("serviceTask");
        String id=flowNode.getId();
        if (incoming != null && !incoming.isEmpty()) {
            Properties props = flowNode.getProps();
            String type = props.getType();
            if("WEBHOOK".equals(type)){
                ServiceTask serviceTask = new ServiceTask();
                serviceTask.setName(flowNode.getName());
                serviceTask.setId(id);
                serviceTask.setImplementationType(IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                serviceTask.setImplementation("${triggerTaskExListener}");
                process.addFlowElement(serviceTask);
                process.addFlowElement(connect(incoming.get(0), id,sequenceFlows,childNodeMap,process));
            }
            //邮件节点的逻辑,不需要变
            else{
                EmailInfo email = props.getEmail();
                ServiceTask serviceTask=new ServiceTask();
                serviceTask.setType("mail");
                List<FieldExtension> fieldExtensions= new ArrayList<>();

                FieldExtension emailFrom= new FieldExtension();
                emailFrom.setFieldName("from");
                emailFrom.setStringValue("2471089198@qq.com");
                fieldExtensions.add(emailFrom);

                FieldExtension emailTo= new FieldExtension();
                emailTo.setFieldName("to");
                emailTo.setStringValue(StrUtil.join(",",email.getTo()));
                fieldExtensions.add(emailTo);

                FieldExtension emailSubject= new FieldExtension();
                emailSubject.setFieldName("subject");
                emailSubject.setStringValue(email.getSubject());
                fieldExtensions.add(emailSubject);

                FieldExtension emailContent= new FieldExtension();
                emailContent.setFieldName("text");
                String content = email.getContent();
                try {
                    content = Base64.encode(content.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                emailContent.setExpression("${"+EXPRESSION_CLASS+ "mailContent(execution,"+"'"+content+"'"+")"+"}");
                fieldExtensions.add(emailContent);
                serviceTask.setName(flowNode.getName());
                serviceTask.setId(id);
                serviceTask.setFieldExtensions(fieldExtensions);
                process.addFlowElement(serviceTask);
                process.addFlowElement(connect(incoming.get(0), id,sequenceFlows,childNodeMap,process));
            }

        }
        return id;
    }

    private static String createDelayTask(Process process,ChildNode flowNode,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) {
        JSONObject incomingJson = flowNode.getIncoming();
        List<String> incoming = incomingJson.getJSONArray("incoming").toJavaList(String.class);
        // 自动生成id
//        String id = id("serviceTask");
        String id=flowNode.getId();
        if (incoming != null && !incoming.isEmpty()) {
            Properties props = flowNode.getProps();
            String type = props.getType();
            IntermediateCatchEvent intermediateCatchEvent = new IntermediateCatchEvent();
            intermediateCatchEvent.setName(flowNode.getName());
            intermediateCatchEvent.setId(id);
            process.addFlowElement(intermediateCatchEvent);
            process.addFlowElement(connect(incoming.get(0), id,sequenceFlows,childNodeMap,process));
            TimerEventDefinition timerEventDefinition = new TimerEventDefinition();
            if("FIXED".equals(type)){
                Long time = props.getTime();
                String unit = props.getUnit();
                if("D".equals(unit)){
                    timerEventDefinition.setTimeDuration("P"+time+unit);
                }
                else{
                    timerEventDefinition.setTimeDuration("PT"+time+unit);
                }
                timerEventDefinition.setId(id("timerEventDefinition"));
                intermediateCatchEvent.addEventDefinition(timerEventDefinition);
            }
            else{
                String dateTime = props.getDateTime();
                timerEventDefinition.setTimeDate("${"+EXPRESSION_CLASS+"timeDate(execution)}");
                intermediateCatchEvent.addEventDefinition(timerEventDefinition);
            }
        }
        return id;
    }
    private static String createTriggerTask(Process process,ChildNode flowNode,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) {
        JSONObject incomingJson = flowNode.getIncoming();
        List<String> incoming = incomingJson.getJSONArray("incoming").toJavaList(String.class);
        // 自动生成id
//        String id = id("serviceTask");
        String id=flowNode.getId();
        if (incoming != null && !incoming.isEmpty()) {
            Properties props = flowNode.getProps();
            String type = props.getType();
            if("WEBHOOK".equals(type)){
                HttpInfo http = props.getHttp();
                HttpServiceTask serviceTask= new HttpServiceTask();
                serviceTask.setType("http");
                List<FieldExtension> fieldExtensions= new ArrayList<>();
                FieldExtension requestMethod= new FieldExtension();
                requestMethod.setFieldName("requestMethod");
                requestMethod.setStringValue(http.getMethod());
                fieldExtensions.add(requestMethod);

                FieldExtension requestUrl= new FieldExtension();
                requestUrl.setFieldName("requestUrl");
                requestUrl.setStringValue(http.getUrl());
                fieldExtensions.add(requestUrl);

                List<Map<String, Object>> headers = http.getHeaders();
                Map<String,Object> header= new HashMap<>();
                header.put("isField",false);
                header.put("name","Content-Type");
                header.put("value","application/json");
                headers.add(header);

                FieldExtension requestHeaders= new FieldExtension();
                requestHeaders.setFieldName("requestHeaders");
                String s = JSONObject.toJSONString(headers);
                try {
                    s = cn.hutool.core.codec.Base64.encode(s.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                requestHeaders.setExpression("${"+EXPRESSION_CLASS+ "requestHeaders(execution,"+"'"+s+"'"+")"+"}");
                fieldExtensions.add(requestHeaders);

                List<Map<String, Object>> params = http.getParams();
                FieldExtension requestBody= new FieldExtension();
                requestBody.setFieldName("requestBody");
                String bodyStr = JSONObject.toJSONString(params);
                try {
                    bodyStr = cn.hutool.core.codec.Base64.encode(bodyStr.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                requestBody.setExpression("${"+EXPRESSION_CLASS+ "requestBody(execution,"+"'"+bodyStr+"'"+")"+"}");
                fieldExtensions.add(requestBody);

                FieldExtension requestCharset= new FieldExtension();
                requestCharset.setFieldName("requestBodyEncoding");
                requestCharset.setStringValue("UTF-8");
                fieldExtensions.add(requestCharset);
                serviceTask.setFieldExtensions(fieldExtensions);

                serviceTask.setName(flowNode.getName());
                serviceTask.setId(id);

                process.addFlowElement(serviceTask);
                process.addFlowElement(connect(incoming.get(0), id,sequenceFlows,childNodeMap,process));
            }
            else{
                EmailInfo email = props.getEmail();
                ServiceTask serviceTask=new ServiceTask();
                serviceTask.setType("mail");
                List<FieldExtension> fieldExtensions= new ArrayList<>();

                FieldExtension emailFrom= new FieldExtension();
                emailFrom.setFieldName("from");
                emailFrom.setStringValue("2471089198@qq.com");
                fieldExtensions.add(emailFrom);

                FieldExtension emailTo= new FieldExtension();
                emailTo.setFieldName("to");
                emailTo.setStringValue(StrUtil.join(",",email.getTo()));
                fieldExtensions.add(emailTo);

                FieldExtension emailSubject= new FieldExtension();
                emailSubject.setFieldName("subject");
                emailSubject.setStringValue(email.getSubject());
                fieldExtensions.add(emailSubject);

                FieldExtension emailContent= new FieldExtension();
                emailContent.setFieldName("text");
                String content = email.getContent();
                try {
                    content = Base64.encode(content.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                emailContent.setExpression("${"+EXPRESSION_CLASS+ "mailContent(execution,"+"'"+content+"'"+")"+"}");
                fieldExtensions.add(emailContent);
                serviceTask.setName(flowNode.getName());
                serviceTask.setId(id);
                serviceTask.setFieldExtensions(fieldExtensions);
                process.addFlowElement(serviceTask);
                process.addFlowElement(connect(incoming.get(0), id,sequenceFlows,childNodeMap,process));
            }

        }
        return id;
    }


    private static String createExclusiveGatewayBuilder(String formId,  ChildNode flowNode,Process process,BpmnModel bpmnModel,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        childNodeMap.put(flowNode.getId(),flowNode);
        String name =flowNode.getName();
        String exclusiveGatewayId = flowNode.getId();
        ExclusiveGateway exclusiveGateway = new ExclusiveGateway();
        exclusiveGateway.setId(exclusiveGatewayId);
        exclusiveGateway.setName(name);
        process.addFlowElement(exclusiveGateway);
        process.addFlowElement(connect(formId, exclusiveGatewayId,sequenceFlows,childNodeMap,process));

        if (Objects.isNull(flowNode.getBranchs()) && Objects.isNull(flowNode.getChildren())) {
            return exclusiveGatewayId;
        }
        List<ChildNode> flowNodes = flowNode.getBranchs();
        List<String> incoming = Lists.newArrayListWithCapacity(flowNodes.size());
        List<JSONObject> conditions = Lists.newCopyOnWriteArrayList();
        for (ChildNode element : flowNodes) {
            Boolean typeElse = element.getTypeElse();
            if(Boolean.TRUE.equals(typeElse)){
                exclusiveGateway.setDefaultFlow(element.getId());
            }
            childNodeMap.put(element.getId(),element);
            ChildNode childNode = element.getChildren();

            String nodeName = element.getName();
            Properties props = element.getProps();
            String expression = props.getExpression();


            if (Objects.isNull(childNode) ||  StringUtils.isBlank(childNode.getId())) {

                incoming.add(exclusiveGatewayId);
                JSONObject condition = new JSONObject();
                condition.fluentPut("nodeName", nodeName)
                        .fluentPut("expression", expression)
                        .fluentPut("groups",props.getGroups())
                        .fluentPut("groupsType",props.getGroupsType()
                                )
                        .fluentPut("elseSequenceFlowId",element.getId());
                conditions.add(condition);
                continue;
            }
            // 只生成一个任务，同时设置当前任务的条件
            JSONObject incomingObj = childNode.getIncoming();
            incomingObj.put("incoming", Collections.singletonList(exclusiveGatewayId));
            String identifier = create(exclusiveGatewayId, childNode,process,bpmnModel,sequenceFlows,childNodeMap);
            List<SequenceFlow> flows = sequenceFlows.stream().filter(flow -> StringUtils.equals(exclusiveGatewayId, flow.getSourceRef()))
                    .collect(Collectors.toList());
            flows.stream().forEach(
                    e -> {
                        if (StringUtils.isBlank(e.getName()) && StringUtils.isNotBlank(nodeName)) {
                            e.setName(nodeName);
                        }
                        // 设置条件表达式
                        if (Objects.isNull(e.getConditionExpression()) && StringUtils.isNotBlank(expression)) {
                            e.setConditionExpression(expression);
                        }
                    }
            );
            if (Objects.nonNull(identifier)) {
                incoming.add(identifier);
            }
        }


        ChildNode childNode = flowNode.getChildren();

        if (Objects.nonNull(childNode) &&StringUtils.isNotBlank(childNode.getId()) ) {
            String parentId = childNode.getParentId();
            ChildNode parentChildNode = childNodeMap.get(parentId);
            boolean conFlag = Type.CONCURRENTS.type
                .equals(parentChildNode.getType());
            if(!conFlag) {
                String type = childNode.getType();
                if(!Type.EMPTY.type.equals(type)){
                }
                else{
                    if(Type.CONDITIONS.type.equals(parentChildNode.getType())){
                      String endExId=  parentChildNode.getId()+"end";
                      process.addFlowElement(createExclusiveGateWayEnd(endExId));
                        if (incoming == null || incoming.isEmpty()) {
                            return create(exclusiveGatewayId, childNode, process, bpmnModel, sequenceFlows,
                                childNodeMap);
                        }
                        else {
                            JSONObject incomingObj = childNode.getIncoming();
                            // 所有 service task 连接 end exclusive gateway
                            incomingObj.put("incoming", incoming);
                            FlowElement flowElement = bpmnModel.getFlowElement(incoming.get(0));
                            // 1.0 先进行边连接, 暂存 nextNode
                            ChildNode nextNode = childNode.getChildren();
                            childNode.setChildren(null);
                            String identifier = endExId;
                            for (int i = 0; i < incoming.size(); i++) {
                                process.addFlowElement(connect(incoming.get(i), identifier, sequenceFlows,childNodeMap,process));
                            }

                            //  针对 gateway 空任务分支 添加条件表达式
                            if (!conditions.isEmpty()) {
                                FlowElement flowElement1 = bpmnModel.getFlowElement(identifier);
                                // 获取从 gateway 到目标节点 未设置条件表达式的节点
                                List<SequenceFlow> flows = sequenceFlows.stream().filter(
                                    flow -> StringUtils.equals(flowElement1.getId(), flow.getTargetRef()))
                                    .filter(
                                        flow -> StringUtils.equals(flow.getSourceRef(), exclusiveGatewayId))
                                    .collect(Collectors.toList());
                                flows.stream().forEach(sequenceFlow -> {
                                    if (!conditions.isEmpty()) {
                                        JSONObject condition = conditions.get(0);
                                        String nodeName = condition.getString("nodeName");
                                        String expression = condition.getString("expression");

                                        if (StringUtils.isBlank(sequenceFlow.getName()) && StringUtils
                                            .isNotBlank(nodeName)) {
                                            sequenceFlow.setName(nodeName);
                                        }
                                        // 设置条件表达式
                                        if (Objects.isNull(sequenceFlow.getConditionExpression())
                                            && StringUtils.isNotBlank(expression)) {
                                            sequenceFlow.setConditionExpression(expression);
                                        }

                                        FlowElement flowElement2 = process.getFlowElement(sequenceFlow.getId());
                                        if(flowElement2!=null){
                                            flowElement2.setId(condition.getString("elseSequenceFlowId"));
                                            exclusiveGateway.setDefaultFlow(flowElement2.getId());;
                                        }

                                        conditions.remove(0);
                                    }
                                });

                            }

                            // 1.1 边连接完成后，在进行 nextNode 创建
                            if (Objects.nonNull(nextNode) &&StringUtils.isNotBlank(nextNode.getId())) {
                                return create(identifier, nextNode, process, bpmnModel, sequenceFlows,
                                    childNodeMap);
                            } else {
                                return identifier;
                            }
                        }


                    }
                }
            }
            else{
                System.err.println("-");
            }
        }
        return exclusiveGatewayId;
    }

    private static String createInclusiveGatewayBuilder(String formId,  ChildNode flowNode,Process process,BpmnModel bpmnModel,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        childNodeMap.put(flowNode.getId(),flowNode);
        String name =flowNode.getName();
        String exclusiveGatewayId = flowNode.getId();
        InclusiveGateway exclusiveGateway = new InclusiveGateway();
        exclusiveGateway.setId(exclusiveGatewayId);
        exclusiveGateway.setName(name);
        process.addFlowElement(exclusiveGateway);
        process.addFlowElement(connect(formId, exclusiveGatewayId,sequenceFlows,childNodeMap,process));

        if (Objects.isNull(flowNode.getBranchs()) && Objects.isNull(flowNode.getChildren())) {
            return exclusiveGatewayId;
        }
        List<ChildNode> flowNodes = flowNode.getBranchs();
        List<String> incoming = Lists.newArrayListWithCapacity(flowNodes.size());
        List<JSONObject> conditions = Lists.newCopyOnWriteArrayList();
        for (ChildNode element : flowNodes) {
            Boolean typeElse = element.getTypeElse();
            if(Boolean.TRUE.equals(typeElse)){
                exclusiveGateway.setDefaultFlow(element.getId());
            }
            childNodeMap.put(element.getId(),element);
            ChildNode childNode = element.getChildren();

            String nodeName = element.getName();
            Properties props = element.getProps();
            String expression = props.getExpression();


            if (Objects.isNull(childNode) ||  StringUtils.isBlank(childNode.getId())) {

                incoming.add(exclusiveGatewayId);
                JSONObject condition = new JSONObject();
                condition.fluentPut("nodeName", nodeName)
                        .fluentPut("expression", expression)
                        .fluentPut("groups",props.getGroups())
                        .fluentPut("groupsType",props.getGroupsType()
                        )
                        .fluentPut("elseSequenceFlowId",element.getId());
                conditions.add(condition);
                continue;
            }
            // 只生成一个任务，同时设置当前任务的条件
            JSONObject incomingObj = childNode.getIncoming();
            incomingObj.put("incoming", Collections.singletonList(exclusiveGatewayId));
            String identifier = create(exclusiveGatewayId, childNode,process,bpmnModel,sequenceFlows,childNodeMap);
            List<SequenceFlow> flows = sequenceFlows.stream().filter(flow -> StringUtils.equals(exclusiveGatewayId, flow.getSourceRef()))
                    .collect(Collectors.toList());
            flows.stream().forEach(
                    e -> {
                        if (StringUtils.isBlank(e.getName()) && StringUtils.isNotBlank(nodeName)) {
                            e.setName(nodeName);
                        }
                        // 设置条件表达式
                        if (Objects.isNull(e.getConditionExpression()) && StringUtils.isNotBlank(expression)) {
                            e.setConditionExpression(expression);
                        }
                    }
            );
            if (Objects.nonNull(identifier)) {
                incoming.add(identifier);
            }
        }


        ChildNode childNode = flowNode.getChildren();

        if (Objects.nonNull(childNode) &&StringUtils.isNotBlank(childNode.getId()) ) {
            String parentId = childNode.getParentId();
            ChildNode parentChildNode = childNodeMap.get(parentId);
            boolean conFlag = Type.CONCURRENTS.type
                    .equals(parentChildNode.getType());
            if(!conFlag) {
                String type = childNode.getType();
                if(!Type.EMPTY.type.equals(type)){
                }
                else{
                    if(Type.INCLUSIVES.type.equals(parentChildNode.getType())){
                        String endExId=  parentChildNode.getId()+"end";
                        process.addFlowElement(createInclusiveGateWayEnd(endExId));
                        if (incoming == null || incoming.isEmpty()) {
                            return create(exclusiveGatewayId, childNode, process, bpmnModel, sequenceFlows,
                                    childNodeMap);
                        }
                        else {
                            JSONObject incomingObj = childNode.getIncoming();
                            // 所有 service task 连接 end exclusive gateway
                            incomingObj.put("incoming", incoming);
                            FlowElement flowElement = bpmnModel.getFlowElement(incoming.get(0));
                            // 1.0 先进行边连接, 暂存 nextNode
                            ChildNode nextNode = childNode.getChildren();
                            childNode.setChildren(null);
                            String identifier = endExId;
                            for (int i = 0; i < incoming.size(); i++) {
                                process.addFlowElement(connect(incoming.get(i), identifier, sequenceFlows,childNodeMap,process));
                            }

                            //  针对 gateway 空任务分支 添加条件表达式
                            if (!conditions.isEmpty()) {
                                FlowElement flowElement1 = bpmnModel.getFlowElement(identifier);
                                // 获取从 gateway 到目标节点 未设置条件表达式的节点
                                List<SequenceFlow> flows = sequenceFlows.stream().filter(
                                                flow -> StringUtils.equals(flowElement1.getId(), flow.getTargetRef()))
                                        .filter(
                                                flow -> StringUtils.equals(flow.getSourceRef(), exclusiveGatewayId))
                                        .collect(Collectors.toList());
                                flows.stream().forEach(sequenceFlow -> {
                                    if (!conditions.isEmpty()) {
                                        JSONObject condition = conditions.get(0);
                                        String nodeName = condition.getString("nodeName");
                                        String expression = condition.getString("expression");

                                        if (StringUtils.isBlank(sequenceFlow.getName()) && StringUtils
                                                .isNotBlank(nodeName)) {
                                            sequenceFlow.setName(nodeName);
                                        }
                                        // 设置条件表达式
                                        if (Objects.isNull(sequenceFlow.getConditionExpression())
                                                && StringUtils.isNotBlank(expression)) {
                                            sequenceFlow.setConditionExpression(expression);
                                        }

                                        FlowElement flowElement2 = process.getFlowElement(sequenceFlow.getId());
                                        if(flowElement2!=null){
                                            flowElement2.setId(condition.getString("elseSequenceFlowId"));
                                            exclusiveGateway.setDefaultFlow(flowElement2.getId());;
                                        }

                                        conditions.remove(0);
                                    }
                                });

                            }

                            // 1.1 边连接完成后，在进行 nextNode 创建
                            if (Objects.nonNull(nextNode) &&StringUtils.isNotBlank(nextNode.getId())) {
                                return create(identifier, nextNode, process, bpmnModel, sequenceFlows,
                                        childNodeMap);
                            } else {
                                return identifier;
                            }
                        }


                    }
                }
            }
            else{
                System.err.println("-");
            }
        }
        return exclusiveGatewayId;
    }



    public static ExclusiveGateway createExclusiveGateWayEnd(String id){
        ExclusiveGateway exclusiveGateway=new ExclusiveGateway();
        exclusiveGateway.setId(id);
        return exclusiveGateway;
    }

    public static InclusiveGateway createInclusiveGateWayEnd(String id){
        InclusiveGateway exclusiveGateway=new InclusiveGateway();
        exclusiveGateway.setId(id);
        return exclusiveGateway;
    }

    private static ParallelGateway createParallelGateWayEnd(String id){
        ParallelGateway parallelGateway=new ParallelGateway();
        parallelGateway.setId(id);
        return parallelGateway;
    }

    private static String createParallelGatewayBuilder(String formId, ChildNode flowNode,Process process,BpmnModel bpmnModel,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        childNodeMap.put(flowNode.getId(),flowNode);
        String name = flowNode.getName();
        ParallelGateway parallelGateway = new ParallelGateway();
        String parallelGatewayId = flowNode.getId();
        parallelGateway.setId(parallelGatewayId);
        parallelGateway.setName(name);
        process.addFlowElement(parallelGateway);
        process.addFlowElement(connect(formId, parallelGatewayId,sequenceFlows,childNodeMap,process));

        if (Objects.isNull(flowNode.getBranchs()) && Objects.isNull(flowNode.getChildren())) {
            return parallelGatewayId;
        }

        List<ChildNode> flowNodes = flowNode.getBranchs();
        List<String> incoming = Lists.newArrayListWithCapacity(flowNodes.size());
        for (ChildNode element : flowNodes) {
            childNodeMap.put(element.getId(),element);
            ChildNode childNode = element.getChildren();
            if (Objects.isNull(childNode) ||  StringUtils.isBlank(childNode.getId())) {
                incoming.add(parallelGatewayId);
                continue;
            }
            String identifier = create(parallelGatewayId, childNode,process,bpmnModel,sequenceFlows,childNodeMap);
            if (Objects.nonNull(identifier)) {
                incoming.add(identifier);
            }
        }

        ChildNode childNode = flowNode.getChildren();
        if (Objects.nonNull(childNode) &&StringUtils.isNotBlank(childNode.getId())) {
            String parentId = childNode.getParentId();
            ChildNode parentChildNode = childNodeMap.get(parentId);
            boolean conFlag = Type.CONCURRENTS.type
                .equals(parentChildNode.getType());
            if(!conFlag){
                String type = childNode.getType();
                if(!Type.EMPTY.type.equals(type)){

                }
                else{
                    if(Type.CONCURRENTS.type.equals(parentChildNode.getType())){
                        String endExId=  parentChildNode.getId()+"end";
                        process.addFlowElement(createParallelGateWayEnd(endExId));
                        // 普通结束网关
                        if (CollectionUtils.isEmpty(incoming)) {
                            return create(parallelGatewayId, childNode,process,bpmnModel,sequenceFlows,childNodeMap);
                        }
                        else {
                            JSONObject incomingObj = childNode.getIncoming();
                            // 所有 service task 连接 end parallel gateway
                            incomingObj.put("incoming", incoming);
                            FlowElement flowElement = bpmnModel.getFlowElement(incoming.get(0));
                            // 1.0 先进行边连接, 暂存 nextNode
                            ChildNode nextNode = childNode.getChildren();
                            childNode.setChildren(null);
                            String identifier = endExId;
                            for (int i = 0; i < incoming.size(); i++) {
                                FlowElement flowElement1 = bpmnModel.getFlowElement(incoming.get(i));
                                process.addFlowElement(connect(flowElement1.getId(), identifier,sequenceFlows,childNodeMap,process));
                            }
                            // 1.1 边连接完成后，在进行 nextNode 创建
                            if (Objects.nonNull(nextNode)&&StringUtils.isNotBlank(nextNode.getId())) {
                                return create(identifier, nextNode,process,bpmnModel,sequenceFlows,childNodeMap);
                            } else {
                                return identifier;
                            }
                        }
                    }
                }
            }
            else{
                String type = childNode.getType();
                if(!Type.EMPTY.type.equals(type)){

                }
                else{
                    if(Type.CONCURRENTS.type.equals(parentChildNode.getType())){
                        String endExId=  parentChildNode.getId()+"end";
                        process.addFlowElement(createParallelGateWayEnd(endExId));
                        // 普通结束网关
                        if (CollectionUtils.isEmpty(incoming)) {
                            return create(parallelGatewayId, childNode,process,bpmnModel,sequenceFlows,childNodeMap);
                        }
                        else {
                            JSONObject incomingObj = childNode.getIncoming();
                            // 所有 service task 连接 end parallel gateway
                            incomingObj.put("incoming", incoming);
                            FlowElement flowElement = bpmnModel.getFlowElement(incoming.get(0));
                            // 1.0 先进行边连接, 暂存 nextNode
                            ChildNode nextNode = childNode.getChildren();
                            childNode.setChildren(null);
                            String identifier = endExId;
                            for (int i = 0; i < incoming.size(); i++) {
                                FlowElement flowElement1 = bpmnModel.getFlowElement(incoming.get(i));
                                process.addFlowElement(connect(flowElement1.getId(), identifier,sequenceFlows,childNodeMap,process));
                            }
                            // 1.1 边连接完成后，在进行 nextNode 创建
                            if (Objects.nonNull(nextNode) &&StringUtils.isNotBlank(nextNode.getId())) {
                                return create(identifier, nextNode,process,bpmnModel,sequenceFlows,childNodeMap);
                            } else {
                                return identifier;
                            }
                        }
                    }
                }
            }

        }
        return parallelGatewayId;
    }

    private static String createTask(Process process,ChildNode flowNode,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) {
        JSONObject incomingJson = flowNode.getIncoming();
        List<String> incoming = incomingJson.getJSONArray("incoming").toJavaList(String.class);
        // 自动生成id
//        String id = id("serviceTask");
        String id=flowNode.getId();
        if (incoming != null && !incoming.isEmpty()) {
            UserTask userTask = new UserTask();
            userTask.setName(flowNode.getName());
            userTask.setId(id);
            process.addFlowElement(userTask);
            process.addFlowElement(connect(incoming.get(0), id,sequenceFlows,childNodeMap,process));

            ArrayList<FlowableListener> taskListeners = new ArrayList<>();
            FlowableListener taskListener = new FlowableListener();
            // 事件类型,
            taskListener.setEvent(TaskListener.EVENTNAME_CREATE);
            // 监听器类型
            taskListener.setImplementationType(IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            // 设置实现了，这里设置监听器的类型是delegateExpression，这样可以在实现类注入Spring bean.
            taskListener.setImplementation("${taskCreatedListener}");
            taskListeners.add(taskListener);
            userTask.setTaskListeners(taskListeners);
            if("root".equalsIgnoreCase(id)){
                userTask.setAssignee("${initiatorId}");
            }
            else{
                ArrayList<FlowableListener> listeners = new ArrayList<>();
                FlowableListener activitiListener = new FlowableListener();
                // 事件类型,
                activitiListener.setEvent(ExecutionListener.EVENTNAME_START);
                // 监听器类型
                activitiListener.setImplementationType(IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                // 设置实现了，这里设置监听器的类型是delegateExpression，这样可以在实现类注入Spring bean.
                activitiListener.setImplementation("${counterSignListener}");
                listeners.add(activitiListener);
                userTask.setExecutionListeners(listeners);
                Properties props = flowNode.getProps();
                String mode = props.getMode();
                MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics = new MultiInstanceLoopCharacteristics();
                // 审批人集合参数
                multiInstanceLoopCharacteristics.setInputDataItem(userTask.getId()+"assigneeList");
                // 迭代集合
                multiInstanceLoopCharacteristics.setElementVariable("assigneeName");
                // 并行
                multiInstanceLoopCharacteristics.setSequential(false);
                userTask.setAssignee("${assigneeName}");
                // 设置多实例属性
                userTask.setLoopCharacteristics(multiInstanceLoopCharacteristics);
                if(ModeEnums.OR.getTypeName().equals(mode)){
                    multiInstanceLoopCharacteristics.setCompletionCondition("${nrOfCompletedInstances/nrOfInstances > 0}");
                }
                //连续多级主管需要是顺序会签可以达到效果
                else if (ModeEnums.NEXT.getTypeName().equals(mode) || AssigneeTypeEnums.LEADER_TOP.getTypeName().equalsIgnoreCase(props.getAssignedType())){
                    multiInstanceLoopCharacteristics.setSequential(true);
                }

                JSONObject timeLimit = props.getTimeLimit();
                if(timeLimit!=null && !timeLimit.isEmpty()){
                    JSONObject timeout = timeLimit.getJSONObject("timeout");
                    if(timeout!=null && !timeout.isEmpty()){
                        String unit = timeout.getString("unit");
                        Integer value = timeout.getInteger("value");
                        if(value>0){
                            List<BoundaryEvent> boundaryEvents= new ArrayList<>();
                            BoundaryEvent boundaryEvent= new BoundaryEvent();
                            boundaryEvent.setId(id("boundaryEvent"));
                            boundaryEvent.setAttachedToRefId(id);
                            boundaryEvent.setAttachedToRef(userTask);
                            boundaryEvent.setCancelActivity(Boolean.FALSE);
                            TimerEventDefinition timerEventDefinition = new TimerEventDefinition();
                            if("D".equals(unit)){
                                timerEventDefinition.setTimeDuration("P"+value+unit);
                            }
                            else{
                                timerEventDefinition.setTimeDuration("PT"+value+unit);
                            }
                            timerEventDefinition.setId(id("timerEventDefinition"));
                            boundaryEvent.addEventDefinition(timerEventDefinition);
                            FlowableListener flowableListener = new FlowableListener();
                            flowableListener.setEvent(ExecutionListener.EVENTNAME_END);
                            flowableListener.setImplementationType(IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                            flowableListener.setImplementation("${timerListener}");
                            List<FlowableListener> listenerList= new ArrayList<>();
                            listenerList.add(flowableListener);
                            boundaryEvent.setExecutionListeners(listenerList);
                            process.addFlowElement(boundaryEvent);
                            boundaryEvents.add(boundaryEvent);
                            userTask.setBoundaryEvents(boundaryEvents);
                        }
                    }
                }

            }
        }
        return id;
    }

    private static String createServiceTask(Process process,ChildNode flowNode,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) {
        JSONObject incomingJson = flowNode.getIncoming();
        List<String> incoming = incomingJson.getJSONArray("incoming").toJavaList(String.class);
        String id=flowNode.getId();
        if (incoming != null && !incoming.isEmpty()) {
            Properties props = flowNode.getProps();
            String type = props.getType();
            ServiceTask serviceTask = new ServiceTask();
            serviceTask.setName(flowNode.getName());
            serviceTask.setId(id);
            process.addFlowElement(serviceTask);
            process.addFlowElement(connect(incoming.get(0), id,sequenceFlows,childNodeMap,process));
            List<UserInfo> assignedUser = props.getAssignedUser();
            serviceTask.setImplementation("${ccListener}");
            serviceTask.setImplementationType(IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        }
        return id;
    }
    private static String createCallActivity(Process process,ChildNode flowNode,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) {
        JSONObject incomingJson = flowNode.getIncoming();
        List<String> incoming = incomingJson.getJSONArray("incoming").toJavaList(String.class);
        String id=flowNode.getId();
        if (incoming != null && !incoming.isEmpty()) {
            //TODO 待前段完善
            Properties props = flowNode.getProps();
            String type = props.getType();
            CallActivity callActivity = new CallActivity();
            callActivity.setName(flowNode.getName());
            callActivity.setId(id);
            callActivity.setCalledElementType("key");
            String subprocessId = props.getSubprocessId();
            String[] split = subprocessId.replace("[", "").replace("]", "").replace("\"","").split(",");
            String categoryId=split[0];
            String templateId=split[1];
            RepositoryService repositoryService = SpringContextHolder.getBean(RepositoryService.class);
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_PREFIX + templateId).latestVersion().singleResult();
            if(processDefinition==null){
                throw  new WorkFlowException("该流程暂未接入Flowable,请重试");
            }
            callActivity.setInheritVariables(true);
            callActivity.setCalledElement(processDefinition.getKey());
            callActivity.setFallbackToDefaultTenant(true);
            process.addFlowElement(callActivity);
            process.addFlowElement(connect(incoming.get(0), id,sequenceFlows,childNodeMap,process));
            List<UserInfo> assignedUser = props.getAssignedUser();

        }
        return id;
    }

    private enum Type {
        INCLUSIVES("INCLUSIVES", InclusiveGateway.class),
        INCLUSIVE("INCLUSIVE", InclusiveGateway.class),
        /**
         * 并行事件
         */
        CONCURRENTS("CONCURRENTS", ParallelGateway.class),
        CONCURRENT("CONCURRENT", SequenceFlow.class),
        /**
         * 排他事件
         */
        CONDITION("CONDITION", ExclusiveGateway.class),
        CONDITIONS("CONDITIONS", ExclusiveGateway.class),
        IN_CONDITIONS("INCLUSIVES", ExclusiveGateway.class),
        /**
         * 任务
         */
        USER_TASK("APPROVAL", UserTask.class),
        APPROVE_USER_TASK("TASK", UserTask.class),
        EMPTY("EMPTY", Object.class),
        ROOT("ROOT", UserTask.class),
        CC("CC", ServiceTask.class),
        TRIGGER("TRIGGER", ServiceTask.class),
        DELAY("DELAY", IntermediateCatchEvent.class),
        SUBPROCESS("SUBPROCESS", SubProcess.class);
        private String type;

        private Class<?> typeClass;

        Type(String type, Class<?> typeClass) {
            this.type = type;
            this.typeClass = typeClass;
        }

        public final static Map<String, Class<?>> TYPE_MAP = Maps.newHashMap();

        static {
            for (Type element : Type.values()) {
                TYPE_MAP.put(element.type, element.typeClass);
            }
        }

        public boolean isEqual(String type) {
            return this.type.equals(type);
        }

    }

    public static ChildNode getChildNodeByNodeId(String processDefinitionId,String currentActivityId){
        RepositoryService repositoryService = SpringContextHolder.getBean(RepositoryService.class);
        Process mainProcess = repositoryService.getBpmnModel(processDefinitionId).getMainProcess();
        UserTask userTask = (UserTask) mainProcess.getFlowElement(currentActivityId);
        String dingDing = mainProcess.getAttributeValue(FLOWABLE_NAME_SPACE, FLOWABLE_NAME_SPACE_NAME);
        JSONObject jsonObject = JSONObject.parseObject(dingDing, new TypeReference<JSONObject>() {
        });
        String processJson = jsonObject.getString(VIEW_PROCESS_JSON_NAME);
        ChildNode childNode = JSONObject.parseObject(processJson, new TypeReference<ChildNode>(){});
        return getChildNode(childNode, currentActivityId);
    }


    public static  ChildNode getChildNode(ChildNode childNode,String nodeId){
        Map<String,ChildNode> childNodeMap =new HashMap<>();
        if(StringUtils.isNotBlank(childNode.getId())){
            getChildNode(childNode,childNodeMap);
        }

        Set<String> set = childNodeMap.keySet();
        for (String s : set) {
            if(StringUtils.isNotBlank(s)){
                if(s.equals(nodeId)){
                    return childNodeMap.get(s);
                }
            }
        }
        return null;
    }

    private  static  void getChildNode(ChildNode childNode,Map<String,ChildNode> childNodeMap){
        childNodeMap.put(childNode.getId(),childNode);
        List<ChildNode> branchs = childNode.getBranchs();
        ChildNode children = childNode.getChildren();
        if(branchs!=null && branchs.size()>0){
            for (ChildNode branch : branchs) {
                if(StringUtils.isNotBlank(branch.getId())){
                    childNodeMap.put(branch.getId(),branch);
                    getChildNode(branch,childNodeMap);
                }
            }
        }

        if(children!=null ){
            childNodeMap.put(children.getId(),children);
            getChildNode(children,childNodeMap);
        }

    }

}

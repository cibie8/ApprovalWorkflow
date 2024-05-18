package com.dingding.mid.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSort;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;

@RestController
@RequestMapping("input")
@Api(tags = {"Vue2表单的CRUD接口"})
@ApiSort(2)
public class InputController {

    /**
     * 1>
     * @param
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @ApiOperationSupport(order = 0)
    @ApiOperation("填写模板的保存接口(会在此Json转Bpmn)")
    @PostMapping("/list")
    public Object list(){

        return null;
    }

}

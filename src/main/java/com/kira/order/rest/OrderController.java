package com.kira.order.rest;

import com.kira.order.biz.OrderBiz;
import com.kira.order.entity.Order;
import com.kira.order.utils.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: Kira
 * @Description:
 * @Date:Create：in 2020/7/2 3:41 下午
 */
@Slf4j
@RestController
@RequestMapping(value = "/order")
public class OrderController {

    @Autowired
    private OrderBiz orderBiz;

    @GetMapping("/get")
    public Order get(@RequestParam int id){
        return orderBiz.get(id);
    }

    @PostMapping("/call")
    public BaseResponse submitCallOrder(@RequestParam("orderNum") String orderNum,@RequestParam("userName") String userName){
        BaseResponse response = new BaseResponse();
        orderBiz.submitCallOrder(orderNum,userName,response);
        return response;
    }
}

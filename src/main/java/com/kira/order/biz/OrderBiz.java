package com.kira.order.biz;

import com.kira.order.entity.Order;
import com.kira.order.entity.SubOrder;
import com.kira.order.mapper.OrderMapper;
import com.kira.order.mapper.SubOrderMapper;
import com.kira.order.utils.BaseResponse;
import com.kira.order.utils.RedisLockHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Kira
 * @Description:
 * @Date:Create：in 2020/7/2 3:48 下午
 */
@Slf4j
@Service
public class OrderBiz {

    /**
     * 超时时间 5s
     */
    private static final int TIMEOUT = 10*1000;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SubOrderMapper subOrderMapper;

    @Autowired
    private StringRedisTemplate template;

    @Autowired
    RedisLockHelper redisLockHelper;

    public Order get(int id){
        return orderMapper.selectByPrimaryKey(id);
    }

    public void submitCallOrder(String orderNum, String userName, BaseResponse response) {
        String cacheUserOrderNum;
        log.info("----开始催单----");
        log.info("用户：{} 催工单号：{}",userName,orderNum);
        cacheUserOrderNum=template.opsForValue().get(userName+":"+orderNum);
        if(Strings.isNotBlank(cacheUserOrderNum)){
            log.info(userName+"15分钟内已提单；单号："+orderNum);
            response.setMessage(userName+"15分钟内已提单；单号："+orderNum);
            log.info("----结束催单----");
            return ;
        }

        //加锁
        long time = System.currentTimeMillis() + TIMEOUT;
        while(!redisLockHelper.lock(orderNum,String.valueOf(time))){};
        log.info("加锁工单号:{}",orderNum);

        coreCallOrder(orderNum,userName,response);

         //释放锁
        redisLockHelper.unlock(orderNum,String.valueOf(time));
        log.info("释放锁工单号:{}",orderNum);
        log.info("----结束催单----");

}

    //事务隔离级别要取读已提交
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRES_NEW)
    public void coreCallOrder(String orderNum, String userName, BaseResponse response){
        Date date=new Date();

        Order dbOrder=orderMapper.existLatestCallOrder(orderNum);
        if(dbOrder==null){
            Order order = new Order();
            order.setOrderNum(orderNum);
            order.setCrtTime(date);
            order.setUpdTime(date);
            orderMapper.insert(order);
            log.info("催单不存在,新增催单,工单号：{}",orderNum);

            SubOrder subOrder = new SubOrder();
            subOrder.setCrtTime(date);
            subOrder.setCrtUser(userName);
            subOrder.setOrderId(order.getId());
            subOrder.setOrderNum(orderNum);
            subOrderMapper.insert(subOrder);
            log.info("新增催单子记录,用户:{}工单号：{}",userName,orderNum);
            template.opsForValue().set(userName+":"+orderNum,"1", TimeUnit.MINUTES.toMinutes(15));
            log.info("缓存记录key：{}",userName+":"+orderNum);
            response.setMessage(userName+"提单成功；单号："+orderNum);
        }else{
            if(subOrderMapper.existLatestSubCallOrder(orderNum,userName)){
                template.opsForValue().set(userName+":"+orderNum,"1", TimeUnit.MINUTES.toMinutes(15));
                log.info("已存在记录，缓存记录key：{}",userName+":"+orderNum);
                response.setMessage(userName+"15分钟内已提单；单号："+orderNum);
            }else{
                SubOrder subOrder = new SubOrder();
                subOrder.setCrtTime(date);
                subOrder.setCrtUser(userName);
                subOrder.setOrderId(dbOrder.getId());
                subOrder.setOrderNum(orderNum);
                subOrderMapper.insert(subOrder);
                log.info("新增催单子记录,用户:{}工单号：{}",userName,orderNum);
                Order order = new Order();
                order.setId(dbOrder.getId());
                order.setUpdTime(date);
                orderMapper.updateByPrimaryKeySelective(order);
                template.opsForValue().set(userName+":"+orderNum,"1", TimeUnit.MINUTES.toMinutes(15));
                log.info("缓存记录key：{}",userName+":"+orderNum);
                response.setMessage(userName+"提单成功；单号："+orderNum);
            }
        }

    }
}

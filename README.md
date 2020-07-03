　　曾经做过一个类似秒杀系统的模块，系统采用分布式架构，场景是这样子的，用户提了故障单后，有跟踪故障单的需求，衍生了催单，用户会通过多个客户端去提交催单，用户催单有专人去跟踪，有一定的时效性，需要及时处理反馈。

<!--more-->

业务规则

* 同一个工单号一个用户15分钟内只能提一次催单
* 同一个工单号不同用户15分钟内只产生一条催单，新增的用户则追加子记录

**问题**

1. 需要考虑并发操作
2. 保证分布式环境下业务的正确处理
3. 考虑大流量请求下服务处理压力

　　虽然系统是运营商的内部系统，tob的用户量并不会很大，但站在技术角度，需要考虑如果toc要怎么满足的问题。首先处理并发问题，因为多个用户对于同一个工单的业务操作是竞争的，因此需要对同一个工单号上锁，其次要满足高可用，因此在多服务运行环境下，需要引入分布式锁，查了下分布式锁的解决方案可以使用redis或者数据库锁（通过数据库插入记录的唯一性保证），这里使用redis分布式锁来解决，且针对用户的重复查询，使用redis缓存来降低DB的压力，这样15分钟内用户的多次查询都能命中缓存，以此解决问题3。

![流程图](https://github.com/mokitkira/order/blob/master/img/%E7%94%A8%E6%88%B7%E5%B9%B6%E5%8F%91%E5%82%AC%E5%8D%95%E6%B5%81%E7%A8%8B.png)

**伪代码**

```java
void submitCallOrder(String orderNum, String userName){
  String cacheUserOrderNum;
  //缓存取是否催单，key以用户+工单号
  cacheUserOrderNum=template.opsForValue().get(userName+":"+orderNum);
  if(Strings.isNotBlank(cacheUserOrderNum)){
		//用户已催单
  }
  
  //加锁，获取不到一直阻塞
  while(!redisLockHelper.lock(orderNum,String.valueOf(time))){};

  //查询是否有15分钟内的催单
  Order dbOrder=orderMapper.existLatestCallOrder(orderNum);
  if(dbOrder==null){
    //没有则新增催单
    orderMapper.insert(order);
    //新增催单用户子记录
    subOrderMapper.insert(subOrder);
    //缓存数据
    template.opsForValue().set(userName+":"+orderNum,"1", TimeUnit.MINUTES.toMinutes(15));
  }else{
    //用户15分钟内是否催过单
    if(subOrderMapper.existLatestSubCallOrder(orderNum,userName)){
      //缓存数据
      template.opsForValue().set(userName+":"+orderNum,"1", TimeUnit.MINUTES.toMinutes(15));
    }else{
      //新增催单用户子记录
      subOrderMapper.insert(subOrder);
   		//更新催单主记录最近催单时间
      orderMapper.updateByPrimaryKeySelective(order);
      //缓存数据
      template.opsForValue().set(userName+":"+orderNum,"1", TimeUnit.MINUTES.toMinutes(15));
    }
  }
  
  //释放锁
  redisLockHelper.unlock(orderNum,String.valueOf(time));
}
```

**Redis分布式锁工具类**

```java
public class RedisLockHelper {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 加锁
     * @param targetId   targetId - 唯一标志
     * @param timeStamp  当前时间+超时时间 也就是时间戳
     * @return
     */
    public boolean lock(String targetId,String timeStamp){
        if(stringRedisTemplate.opsForValue().setIfAbsent(targetId,timeStamp)){
            // 对应setnx命令，可以成功设置,也就是key不存在
            return true;
        }

        // 判断锁超时 - 防止原来的操作异常，没有运行解锁操作  防止死锁
        String currentLock = stringRedisTemplate.opsForValue().get(targetId);
        // 如果锁过期 currentLock不为空且小于当前时间
        if(!Strings.isNullOrEmpty(currentLock) && Long.parseLong(currentLock) < System.currentTimeMillis()){
            // 获取上一个锁的时间value 对应getset，如果lock存在
            String preLock =stringRedisTemplate.opsForValue().getAndSet(targetId,timeStamp);

            // 假设两个线程同时进来这里，因为key被占用了，而且锁过期了。获取的值currentLock=A(get取的旧的值肯定是一样的),两个线程的timeStamp都是B,key都是K.锁时间已经过期了。
            // 而这里面的getAndSet一次只会一个执行，也就是一个执行之后，上一个的timeStamp已经变成了B。只有一个线程获取的上一个值会是A，另一个线程拿到的值是B。
            if(!Strings.isNullOrEmpty(preLock) && preLock.equals(currentLock) ){
                // preLock不为空且preLock等于currentLock，也就是校验是不是上个对应的商品时间戳，也是防止并发
                return true;
            }
        }
        return false;
    }


    /**
     * 解锁
     * @param target
     * @param timeStamp
     */
    public void unlock(String target,String timeStamp){
        try {
            String currentValue = stringRedisTemplate.opsForValue().get(target);
            if(!Strings.isNullOrEmpty(currentValue) && currentValue.equals(timeStamp) ){
                // 删除锁状态
                stringRedisTemplate.opsForValue().getOperations().delete(target);
            }
        } catch (Exception e) {
            log.error("警报！警报！警报！解锁异常{}",e);
        }
    }
}
```


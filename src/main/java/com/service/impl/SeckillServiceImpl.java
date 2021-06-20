package com.service.impl;

import com.dao.SeckillDao;
import com.dao.SuccessSeckillDao;
import com.dao.cache.RedisDao;
import com.dto.Exposer;
import com.dto.SeckillExecution;
import com.enums.SeckillStatEnum;
import com.exception.RepeatKillException;
import com.exception.SeckillCloseException;
import com.exception.SeckillException;
import com.pojo.Seckill;
import com.pojo.SuccessKilled;
import com.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;

@Service
public class SeckillServiceImpl implements SeckillService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillDao seckillDao;
    @Autowired
    private SuccessSeckillDao successSeckillDao;
    /* 加入一个md5盐值字符串,用于混淆*/
    private final String slat = "thisIsASaltValue";

    @Autowired
    private RedisDao redisDao;

    @Override
    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0,4);
    }

    @Override
    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    @Override
    public Exposer exportSeckillUrl(long seckillId) {

        //优化点：缓存优化:超时的基础上维护一致性
        //1.访问redis
        Seckill seckill = redisDao.getSeckill(seckillId);
        if (seckill == null){
            seckill = seckillDao.queryById(seckillId);
            if (seckill == null)
                return new Exposer(false, seckillId);
            else
                redisDao.putSeckill(seckill);
        }

//        Seckill seckill = seckillDao.queryById(seckillId);
//        if (seckill == null){
//            // ??
//            return new Exposer(true, seckillId);
//        }
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        Date nowTime = new Date();
        if (nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()){
            // 还未到秒杀时间/秒杀时间已过
            return new Exposer(false, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());
        }
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);
    }

    private String getMD5(long seckillId) {
        String base = seckillId + "/" + slat;//通过加上混淆的md5不能被别人破解
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());//加密
        return md5;
    }

    /**
     * 使用注解控制事务方法的优点：
     * 1.开发团队达成一致约定，明确标注事务方法的编程风格
     * 2.保证事务方法的执行时间尽可能短，不要穿插其他的网络操作，RPC/HTTP请求或者剥离到事务方法外部
     * 3.不是所有的方法都需要事务。如只有一条修改操作，只读操作不需要事务控制
     */
    @Override
    @Transactional
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        try {
            if (md5.equals(null) || !md5.equals(getMD5(seckillId))){
                logger.error("秒杀数据被篡改");
                throw new SeckillException("seckill data rewrite");
            }
            //执行秒杀逻辑：减库存+记录购买行为
            Date nowTime = new Date();
            System.out.println(nowTime.getTime());
            //代码调整，先insert后update，减少获取rowlock的时间，优化性能
            int insertResult = successSeckillDao.insertSuccessKilled(seckillId, userPhone);
            if (insertResult <= 0) {
                throw new RepeatKillException("seckill repeat");
            }
//            int reduceResult = seckillDao.reduceNumber(seckillId, nowTime);
//            if (reduceResult <= 0){
//                logger.warn("没有更新数据库记录,说明秒杀结束");
//                throw new SeckillCloseException("seckill is closed");
            else {
//                int insertResult = successSeckillDao.insertSuccessKilled(seckillId, userPhone);
//                if (insertResult <= 0){
//                    throw new RepeatKillException("seckill repeat");
                //减库存
                int reduceResult = seckillDao.reduceNumber(seckillId, nowTime);
                if (reduceResult <= 0){
                    logger.warn("没有更新数据库记录,说明秒杀结束");
                    throw new SeckillCloseException("seckill is closed");//rollback
                }else {
                    // 秒杀成功了,返回那条插入成功秒杀的信息
                    SuccessKilled successKilled = successSeckillDao.queryByIdWithSeckill(seckillId, userPhone);
                    //把秒杀成功这种常量字符串放入数据字典，使用枚举
                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
                }
            }
        }catch (SeckillCloseException e1){
            throw e1;
        }catch (RepeatKillException e2){
            throw e2;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            //把编译期异常转换为运行时异常
            //rollback回滚
            throw new SeckillException("seckill inner error : " + e.getMessage());
        }
    }
}

package com.controller;

import com.dto.Exposer;
import com.dto.SeckillExecution;
import com.dto.SeckillResult;
import com.enums.SeckillStatEnum;
import com.exception.RepeatKillException;
import com.exception.SeckillCloseException;
import com.exception.SeckillException;
import com.pojo.Seckill;
import com.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.jws.WebParam;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/seckill")  //url:/模块/资源/{id}/细分  /seckill/list
public class SeckillController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SeckillService seckillService;

    /**
     * 进入秒杀列表.
     *
     * @param model 模型数据,里面放置有秒杀商品的信息
     * @return 秒杀列表详情页面
     */
    @RequestMapping(value = {"/list", "", "index"})
    public String list(Model model){
        List<Seckill> seckillList = seckillService.getSeckillList();
        model.addAttribute("list", seckillList);
        return "list";
    }

    @RequestMapping(value = "/{seckillId}/detail")
    public String detail(@PathVariable("seckillId") Long seckillId, Model model){
        if (seckillId == null)
            return "redirect:/seckill/list";
        Seckill seckill = seckillService.getById(seckillId);
        // 不存在此秒杀商品时
        if (seckill == null)
            return "forward:/seckill/list";
        model.addAttribute("seckill", seckill);
        return "detail";
    }

    /**
     * 暴露秒杀接口的方法.
     *
     * @param seckillId 秒杀商品的id
     * @return 根据用户秒杀的商品id进行业务逻辑判断, 返回不同的json实体结果
     */
    @RequestMapping(value = "/{seckillId}/exposer", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(@PathVariable("seckillId") Long seckillId){
        // 查询秒杀商品的结果
        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        SeckillResult<Exposer> exposerSeckillResult = new SeckillResult<>(true, exposer);

        return exposerSeckillResult;
    }

    /**
     * 用户执行秒杀,在页面点击相应的秒杀连接,进入后获取对应的参数进行判断,返回相对应的json实体结果,前端再进行处理.
     *
     * @param seckillId 秒杀的商品,对应的时秒杀的id
     * @param md5       一个被混淆的md5加密值
     * @param userPhone 参与秒杀用户的额手机号码,当做账号密码使用
     * @return 参与秒杀的结果, 为json数据
     */
    @RequestMapping(value = "/{seckillId}/{md5}/execution", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") long seckillId,
                                                   @PathVariable("md5") String md5,
                                                   @CookieValue(value = "userPhone", required = false) Long userPhone){
        //required = false使得userPhone不是必须的，缺少时不会报错
        // 如果用户的手机号码为空的说明没有填写手机号码进行秒杀
        if (userPhone == null)
            return new SeckillResult<>(false, "没有注册");
        // 根据用户的手机号码,秒杀商品的id跟md5进行秒杀商品,没异常就是秒杀成功
        try {
//            SeckillExecution execution = seckillService.executeSeckill(seckillId, userPhone, md5);
            SeckillExecution execution = seckillService.executeSeckillProcedure(seckillId, userPhone, md5);
            return new SeckillResult<>(true, execution);
        }catch (RepeatKillException e1){
            // 重复秒杀
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStatEnum.REPEAT_KILL);
            return new SeckillResult<>(true, execution);
        }catch (SeckillCloseException e2){
            // 秒杀关闭
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStatEnum.END);
            return new SeckillResult<>(true, execution);
        }catch (SeckillException e){
            // 不能判断的异常
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStatEnum.INNER_ERROR);
            return new SeckillResult<>(true, execution);
        }
    }

    /**
     * 获取服务器端时间,防止用户篡改客户端时间提前参与秒杀
     *
     * @return 时间的json数据
     */
    @RequestMapping(value = "/time/now", method = RequestMethod.GET)
    @ResponseBody
    public SeckillResult<LocalDateTime> time(){
        Date date = new Date();
        return new SeckillResult(true, date.getTime());
    }

}

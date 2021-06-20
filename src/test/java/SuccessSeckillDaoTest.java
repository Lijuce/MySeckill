import com.dao.SeckillDao;
import com.dao.SuccessSeckillDao;
import com.pojo.SuccessKilled;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.pojo.Seckill;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 配置spring和junit整合，junit启动时加载springIOC容器
 * spring-test,junit
 */
@RunWith(SpringJUnit4ClassRunner.class)
//告诉junit spring配置文件
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SuccessSeckillDaoTest {

    //注入Dao实现类依赖
    @Resource
    private SuccessSeckillDao successSeckillDao;

    @Test
    public void insertSuccessKilled() throws Exception {
        long id = 1000l;
        long phone = 13133333343l;
        int res = successSeckillDao.insertSuccessKilled(id, phone);
        System.out.println(res);
    }

    @Test
    public void queryByIdWithSeckill(){
        long id = 1000l;
        long phone = 13133333333l;
        SuccessKilled successKilled = successSeckillDao.queryByIdWithSeckill(id, phone);
        System.out.println(successKilled);
        System.out.println(successKilled.getSeckill());
    }
}
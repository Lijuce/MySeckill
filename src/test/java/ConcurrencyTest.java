
import com.Utility.ConstValue;
import com.Utility.CookieUtility;
import com.service.SeckillService;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.*;

@RunWith(SpringJUnit4ClassRunner.class)
//告诉junit spring的配置文件
@ContextConfiguration({"classpath:/spring/spring-*.xml"})
public class ConcurrencyTest {
    @Autowired
    SeckillService secKillService;

    /**
     *测试并发的入口
     */
    @Test
    public void simulateConcurrency(){
        try {
            calculateTime(1000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }


    /**
     * 模拟请求
     * @param userNum 并发数
     * @throws InterruptedException
     */
    public void calculateTime(int userNum) throws InterruptedException {
        long startTime=System.currentTimeMillis();
        ExecutorService service= Executors.newFixedThreadPool(userNum);
        for (int i = 0; i < userNum; i++){
            int finalI = i;
            service.execute(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        request(13500000000l+ finalI);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }));
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.HOURS);
        long endTime=System.currentTimeMillis();
        System.out.println("耗费时间："+(endTime-startTime)/1000);
    }

    public void  request(long phone) throws IOException {
        //高并发请求测试
        int secID=1000;
        String urlPath = "http://localhost:8080/seckill/"+secID+"/"
                +CookieUtility.getMd5(secID)+"/execution";
        long userKey = phone;
        String result = "";
        BasicCookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
        RequestConfig requestConfig =  RequestConfig.custom().setSocketTimeout(1000000).setConnectTimeout(1000000).build();
        try {
            HttpPost post = new HttpPost(urlPath);//这里发送post请求
            post.setConfig(requestConfig);
            // 每个用户的cookie创建
            List<BasicClientCookie> cookies=createCookie(userKey);
            for (BasicClientCookie cookie:cookies){
                cookieStore.addCookie(cookie);
            }
            // 通过请求对象获取响应对象
            CloseableHttpResponse response = httpClient.execute(post);
            // 判断网络连接状态码是否正常(0--200都数正常)
            result = EntityUtils.toString(response.getEntity(), "utf-8");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    List<BasicClientCookie> createCookie(String userName){
        List<BasicClientCookie> cookies=new ArrayList<>();
        //用户名
        BasicClientCookie cookie = new BasicClientCookie(ConstValue.USER_KEY, userName);
        cookie.setDomain("localhost");
        cookie.setPath("/seckill/");
        cookies.add(cookie);
        //ssid
        BasicClientCookie ssID = new BasicClientCookie(ConstValue.SS_ID, CookieUtility.getMd5(userName));
        ssID.setDomain("localhost");
        ssID.setPath("/seckill/");
        cookies.add(cookie);
        // 手机号码
        BasicClientCookie phone = new BasicClientCookie(ConstValue.USER_KEY, userName);
        cookie.setDomain("localhost");
        cookie.setPath("/seckill/");
        cookies.add(cookie);
        return cookies;
    }

    List<BasicClientCookie> createCookie(long phone){
        List<BasicClientCookie> cookies=new ArrayList<>();

        // 手机号码
        BasicClientCookie phoneCookie = new BasicClientCookie(ConstValue.USER_Phone, String.valueOf(phone));
        phoneCookie.setDomain("localhost");
        phoneCookie.setPath("/seckill/");
        cookies.add(phoneCookie);
        return cookies;
    }
}


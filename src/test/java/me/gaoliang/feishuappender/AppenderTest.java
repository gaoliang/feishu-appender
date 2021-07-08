package me.gaoliang.feishuappender;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.slf4j.MDC;

/**
 * @author ziyuan
 */
@Slf4j
public class AppenderTest {
    @Test
    public void test() throws InterruptedException {
        log.debug("this is a log and level is debug");
        log.info("this is a log and level is info");
        log.warn("this is a log and level is warn");
        // MDC extras
        MDC.put("变量1", "Development");
        MDC.put("变量2", "Linux");

        // This sends an event where the Environment and OS MDC values are set as additional data
        log.error("MDC 例子");

        for (int i = 0; i < 100; i++) {
            try{
                int a = 0;
                a = 1 / a;
            } catch (Exception e) {
                log.error("除0错误 {}", i,e);
            }
        }
        for (int i = 0; i < 100; i++) {
            try{
                int a = 0;
                throw new ClassNotFoundException();
            } catch (Exception e) {
                log.error("测试message模版： {}", i);
            }
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

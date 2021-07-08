package me.gaoliang.feishuappender.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.google.common.collect.Lists;
import com.larksuite.appframework.sdk.AppConfiguration;
import com.larksuite.appframework.sdk.LarkAppInstance;
import com.larksuite.appframework.sdk.LarkAppInstanceFactory;
import com.larksuite.appframework.sdk.client.BatchMessageDestination;
import com.larksuite.appframework.sdk.client.LarkClient;
import com.larksuite.appframework.sdk.client.message.CardMessage;
import com.larksuite.appframework.sdk.client.message.card.Card;
import com.larksuite.appframework.sdk.client.message.card.Config;
import com.larksuite.appframework.sdk.client.message.card.Header;
import com.larksuite.appframework.sdk.client.message.card.element.Button;
import com.larksuite.appframework.sdk.client.message.card.module.Action;
import com.larksuite.appframework.sdk.client.message.card.module.Div;
import com.larksuite.appframework.sdk.client.message.card.module.Hr;
import com.larksuite.appframework.sdk.client.message.card.module.Module;
import com.larksuite.appframework.sdk.client.message.card.objects.Field;
import com.larksuite.appframework.sdk.client.message.card.objects.Text;
import com.larksuite.appframework.sdk.client.message.card.objects.Url;
import com.larksuite.appframework.sdk.exception.LarkClientException;
import me.gaoliang.feishuappender.core.Digest;
import me.gaoliang.feishuappender.core.DigestMessage;
import me.gaoliang.feishuappender.spi.Message;
import me.gaoliang.feishuappender.spi.MessageWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author gaoliang
 */
@Slf4j
public class FeishuMessageWriter implements MessageWriter<ILoggingEvent> {

    private final LarkClient larkClient;
    private String hostname = "";

    // TODO: 请修改
    private static final String APP_ID = "";
    private static final String APP_SECRET = "";
    private static final String ENCRYPT_KEY = "";
    private static final String VERIFICATION_TOKEN = "";
    private static final String API_BASH_PATH = "https://open.feishu.cn/";


    FeishuMessageWriter() {

        try {
            InetAddress ia = InetAddress.getLocalHost();
            this.hostname = ia.getHostName();
        } catch (UnknownHostException e) {
            log.error("FeishuMessageWriter|get hostname error, {}", hostname);
        }
        AppConfiguration ac = new AppConfiguration();
        // TODO: 在这里设置APP配置
        ac.setAppShortName("feishu-appender");
        ac.setAppId(APP_ID);
        ac.setAppSecret(APP_SECRET);
        ac.setEncryptKey(ENCRYPT_KEY);
        ac.setVerificationToken(VERIFICATION_TOKEN);
        ac.setIsIsv(false);

        LarkAppInstance ins = LarkAppInstanceFactory
                .builder(ac)
                .apiBasePath(API_BASH_PATH)
                .create();
        larkClient = ins.getLarkClient();
    }

    @Override
    public void logThrough(Message<ILoggingEvent> message, String projectName, List<String> userIds) {
        // error 日志的message
        String logMessage = message.getEvent().getFormattedMessage();
        String title = projectName + "报错 " + logMessage;
        IThrowableProxy throwableProxy = message.getEvent().getThrowableProxy();

        Card card = new Card(
                new Config(true),
                new Header(new Text(Text.Mode.PLAIN_TEXT, title)));

        // 基础字段
        List<Field> fields = new ArrayList<>();
        fields.add(
                new Field(
                        new Text(Text.Mode.LARK_MD, "**机器：**" + hostname),
                        false
                )
        );
        LocalDateTime now = LocalDateTime.now();
        fields.add(
                new Field(
                        new Text(Text.Mode.LARK_MD, "**推送时间：**" + now),
                        false
                )
        );
        fields.add(
                new Field(
                        new Text(Text.Mode.LARK_MD, "**日志message：**" + logMessage),
                        false
                )
        );

        // 如果有异常信息，则带着堆栈发出去
        if (throwableProxy != null) {

            fields.add(
                    new Field(
                            new Text(Text.Mode.LARK_MD, "**异常类：**" + throwableProxy.getClassName()),
                            false
                    )
            );
            fields.add(
                    new Field(
                            new Text(Text.Mode.LARK_MD, "**异常message：**" + throwableProxy.getMessage()),
                            false
                    )
            );

            Div div = new Div(
                    new Text(Text.Mode.LARK_MD, "**异常信息如下：**"),
                    fields, null
            );

            // 错误堆栈
            String fullMessage = message.getFullMessage();
            if (fullMessage.length() > 5000) {
                fullMessage = fullMessage.substring(0, 5000);
            }
            // 先url safe base64
            fullMessage = Base64.getUrlEncoder().withoutPadding().encodeToString(fullMessage.getBytes());

            // 手机和PC的小程序页面不一样
            String phonePath = "pages/index/index?trace=" + fullMessage;
            String pcPath = "pages/index/index?trace=" + fullMessage;

            try {
                phonePath = URLEncoder.encode(phonePath, "UTF-8");
                pcPath = URLEncoder.encode(pcPath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String baseUrl = "https://applink.feishu.cn/client/mini_program/open?appId=" + APP_ID + "&mode=window&path=";


            Url url = new Url(baseUrl + phonePath, baseUrl + phonePath, baseUrl + pcPath);
            Button button = new
                    Button("LogbackCard.btn", new Text(Text.Mode.PLAIN_TEXT, "查看错误堆栈"))
                    .setMultiUrl(url);

            Action action = new Action(Lists.newArrayList(button));
            card.setModules(Lists.newArrayList(
                    div,
                    new Hr(),
                    action
            ));
        } else {
            // 没有异常信息，就发简单的
            Div div = new Div(
                    new Text(Text.Mode.LARK_MD, "**错误信息如下：**"),
                    fields, null
            );

            card.setModules(Lists.newArrayList(
                    div
            ));
        }
        CardMessage msg = new CardMessage(card.toObjectForJson());
        try {
            BatchMessageDestination batchMessageDestination = new BatchMessageDestination();
            batchMessageDestination.setUserIds(userIds);
            this.larkClient.batchSendChatMessage(batchMessageDestination, msg);
        } catch (LarkClientException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void logDigest(Digest digest, String projectName, List<String> userIds) {
        Card card = new Card(
                new Config(true),
                new Header(new Text(Text.Mode.PLAIN_TEXT, projectName + "报错统计: " + this.hostname))
        );

        List<Module> moduleList = new ArrayList<>();

        // 循环这一段时间一类的所有报错类型
        for (int i = 0; i < digest.getDigestMessages().size(); i++) {
            DigestMessage digestMessage = digest.getDigestMessages().get(i);
            List<Field> fields = new ArrayList<>();
            fields.add(
                    new Field(
                            new Text(Text.Mode.LARK_MD, "**\t自上次统计以来**：" + digestMessage.getMessagesSinceLastDigest() + "次"),
                            false
                    )
            );
            fields.add(
                    new Field(
                            new Text(Text.Mode.LARK_MD, "**\t自上次静默以来**：" + digestMessage.getMessagesSinceStart() + "次"),
                            false
                    )
            );
            Div div = new Div(
                    new Text(Text.Mode.LARK_MD, "**" + (i +1) + ": **" + digestMessage.getKeyForDeduplicate()),
                    fields, null
            );
            moduleList.add(div);
            if (i != digest.getDigestMessages().size() -1) {
                moduleList.add(new Hr());
            }
        }

        card.setModules(moduleList);
        CardMessage msg = new CardMessage(card.toObjectForJson());

        try {
            BatchMessageDestination batchMessageDestination = new BatchMessageDestination();
            batchMessageDestination.setUserIds(userIds);
            this.larkClient.batchSendChatMessage(batchMessageDestination, msg);
        } catch (LarkClientException e) {
            e.printStackTrace();
        }
    }
}

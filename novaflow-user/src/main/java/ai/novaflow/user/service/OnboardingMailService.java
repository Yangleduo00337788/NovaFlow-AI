package ai.novaflow.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingMailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${novaflow.alert.mail-from:}")
    private String mailFrom;

    @Value("${novaflow.web.base-url:http://localhost:3000}")
    private String webBaseUrl;

    public boolean sendOwnerInvite(
            String ownerEmail,
            String tenantName,
            String password,
            boolean passwordGenerated) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !StringUtils.hasText(mailHost)) {
            log.info("Skip onboarding invite email: mail sender not configured, email={}", ownerEmail);
            return false;
        }
        if (!StringUtils.hasText(mailFrom)) {
            log.info("Skip onboarding invite email: mail-from not configured, email={}", ownerEmail);
            return false;
        }

        String loginUrl = webBaseUrl.replaceAll("/+$", "") + "/login";
        StringBuilder body = new StringBuilder();
        body.append("您好，\n\n");
        body.append("NovaFlow 平台已为您创建企业「").append(tenantName).append("」。\n");
        body.append("登录地址：").append(loginUrl).append("\n");
        body.append("登录邮箱：").append(ownerEmail).append("\n");
        if (passwordGenerated) {
            body.append("初始密码：").append(password).append("\n");
            body.append("（请登录后尽快修改密码）\n");
        } else {
            body.append("请使用平台运营人员告知的初始密码登录。\n");
        }
        body.append("\n— NovaFlow 平台");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(ownerEmail);
        message.setSubject("NovaFlow 企业账号开通通知");
        message.setText(body.toString());
        sender.send(message);
        return true;
    }
}

package raicod3.example.com.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import raicod3.example.com.dto.email.EmailRequest;
import raicod3.example.com.enums.TokenType;
import raicod3.example.com.exception.BadRequestException;
import raicod3.example.com.model.OTPToken;
import raicod3.example.com.model.User;
import raicod3.example.com.repository.OTPTokenRepository;
import raicod3.example.com.repository.UserRepository;

import java.time.LocalDate;

@Service
@Slf4j
public class NotificationService {


    private final JavaMailSender mailSender;
    private final OTPTokenRepository otpTokenRepository;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;

    public NotificationService(JavaMailSender mailSender, OTPTokenRepository otpTokenRepository, TemplateEngine templateEngine, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.otpTokenRepository = otpTokenRepository;
        this.templateEngine = templateEngine;
        this.userRepository = userRepository;
    }

    public void sendEmail(EmailRequest req, String otpToken, String templatePath) throws MessagingException {


        User user = userRepository.findUserByEmail(req.getEmail()).orElseThrow(() -> new BadRequestException("User not found"));
        otpTokenRepository.save(new OTPToken(otpToken, user, TokenType.PASSWORD_RESET));

        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("otpToken", otpToken);
        context.setVariable("year", LocalDate.now().getYear());

        String htmlContent = templateEngine.process(templatePath, context);

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper messageHelper = new MimeMessageHelper(message, "UTF-8");
        messageHelper.setTo(req.getEmail());
        messageHelper.setSubject(req.getSubject());
        messageHelper.setText(htmlContent, true);

        mailSender.send(message);
    }



}

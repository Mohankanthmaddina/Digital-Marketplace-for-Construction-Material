package com.example.buildpro.service;

import com.example.buildpro.model.OTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOTPEmail(String toEmail, String otpCode, OTP.Purpose purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        // System.out.println("OTP Code:
        // sendOTPEmail*****************************************"+toEmail+" otp:
        // "+otpCode+" OTP.purose: "+purpose);
        message.setSubject(getEmailSubject(purpose));
        message.setText(getEmailContent(otpCode, purpose));
        // System.out.println("OTP Code: sendOTPEmail
        // getContent()*****************************************"+toEmail+" otp:
        // "+otpCode+" OTP.purose: "+purpose);
        message.setFrom("mohankanthmaddina1784@gmail.com");
        // System.out.println("ending !!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        mailSender.send(message);
        System.out.println("Email sent to " + toEmail);
    }

    private String getEmailSubject(OTP.Purpose purpose) {
        switch (purpose) {
            case REGISTRATION:
                return "Verify Your BuildPro Account";
            case PASSWORD_RESET:
                return "Reset Your BuildPro Password";
            default:
                return "BuildPro Notification";
        }
    }

    private String getEmailContent(String otpCode, OTP.Purpose purpose) {
        switch (purpose) {
            case REGISTRATION:
                return "Your OTP for BuildPro registration is: " + otpCode +
                        "\nThis OTP is valid for 10 minutes.";
            case PASSWORD_RESET:
                return "Your OTP for password reset is: " + otpCode +
                        "\nThis OTP is valid for 10 minutes.";
            default:
                return "Your OTP is: " + otpCode;
        }
    }
}

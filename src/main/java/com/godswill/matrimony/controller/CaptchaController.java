package com.godswill.matrimony.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Controller
@RequiredArgsConstructor
public class CaptchaController {

    private final DefaultKaptcha captchaProducer;

    @GetMapping("/captcha-image")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.setDateHeader("Expires", 0);
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.addHeader("Cache-Control", "post-check=0, pre-check=0");
            response.setHeader("Pragma", "no-cache");
            response.setContentType("image/jpeg");

            // Generate CAPTCHA text
            String capText = captchaProducer.createText();

            // Store CAPTCHA text in session
            HttpSession session = request.getSession();
            session.setAttribute("captcha", capText);

            System.out.println("🔐 Generated CAPTCHA: " + capText);

            // Create CAPTCHA image
            BufferedImage bi = captchaProducer.createImage(capText);

            // Write image to response
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(bi, "jpg", out);
            byte[] captchaBytes = out.toByteArray();

            response.getOutputStream().write(captchaBytes);
            response.getOutputStream().flush();

        } catch (Exception e) {
            System.err.println("❌ Error generating CAPTCHA: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
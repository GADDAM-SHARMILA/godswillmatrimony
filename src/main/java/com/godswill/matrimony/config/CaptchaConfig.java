package com.godswill.matrimony.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class CaptchaConfig {

    @Bean
    public DefaultKaptcha captchaProducer() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();

        // CAPTCHA image width
        properties.setProperty("kaptcha.image.width", "200");

        // CAPTCHA image height
        properties.setProperty("kaptcha.image.height", "60");

        // CAPTCHA text font size
        properties.setProperty("kaptcha.textproducer.font.size", "40");

        // CAPTCHA text font color (black)
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0");

        // CAPTCHA text character length
        properties.setProperty("kaptcha.textproducer.char.length", "5");

        // CAPTCHA font names
        properties.setProperty("kaptcha.textproducer.font.names", "Arial,Courier,Times New Roman");

        // CAPTCHA characters to use (numbers and letters)
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        // Remove spaces between characters
        properties.setProperty("kaptcha.textproducer.char.space", "5");

        // Add noise to image
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.DefaultNoise");

        // Noise color
        properties.setProperty("kaptcha.noise.color", "blue");

        // Background color (white)
        properties.setProperty("kaptcha.background.clear.from", "white");
        properties.setProperty("kaptcha.background.clear.to", "white");

        // Border
        properties.setProperty("kaptcha.border", "yes");
        properties.setProperty("kaptcha.border.color", "black");
        properties.setProperty("kaptcha.border.thickness", "1");

        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);

        return defaultKaptcha;
    }
}
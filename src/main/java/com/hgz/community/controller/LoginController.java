package com.hgz.community.controller;

import com.google.code.kaptcha.Producer;
import com.hgz.community.entity.User;
import com.hgz.community.service.UserService;
import com.hgz.community.util.CommunityConstant;
import com.hgz.community.util.CommunityUtil;
import com.hgz.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Controller
public class LoginController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user, String code,
                           @CookieValue("kaptchaOwner") String kaptchaOwner) {
        // 得到验证码， 判断是否存在
        String kaptcha = null;
        if (!StringUtils.isBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确！");
            return "/site/register";
        }

        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活！");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功，您的账号已经可以正常使用了！");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作，该账号已经激活过了！");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败，您提供的激活码不正确！");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 验证码的所有者
        String kaptchaOwner = CommunityUtil.generationUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        // 将验证码写入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败：" + e.getMessage());
        }
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpServletResponse response, @CookieValue("kaptchaOwner") String kaptchaOwner) {
        // 得到验证码， 判断是否存在
        String kaptcha = null;
        if (!StringUtils.isBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确！");
            return "/site/login";
        }

        // 检查账号密码正确性
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECOND : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        if (ticket == null) {
            return "redirect:/login";
        }
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage() {
        return "/site/forget";
    }

    @RequestMapping(path = "/forget", method = RequestMethod.POST)
    public String forget(String email, String code, String password, Model model) {

        // 防止有人捣乱，做好安全后端
        if (StringUtils.isBlank(email)) {
            model.addAttribute("emailMsg", "邮箱不能为空");
            return "/site/forget";
        }
        String redisKey = RedisKeyUtil.getCodeKey(email);
        String redisCode = (String) redisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isBlank(code) || StringUtils.isBlank(redisCode) || !redisCode.equals(code)) {
            model.addAttribute("codeMsg", "验证码不正确！");
            return "/site/forget";
        }

        userService.forgetPassword(userService.getUserByEmail(email), password);

        model.addAttribute("msg", "您的账号密码已经重置！");
        model.addAttribute("target", "/login");
        return "/site/operate-result";
    }

    @RequestMapping(path = "/getCode", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getCode(String email) {

        Map<String, Object> result = new HashMap<>();

        if (StringUtils.isBlank(email)) {
            result.put("code", 1);
            result.put("emailMsg", "邮箱不能为空！");
            return result;
        }

        User user = userService.getUserByEmail(email);
        if (user == null) {
            result.put("code", 1);
            result.put("emailMsg", "该邮箱未注册！");
            return result;
        }

        String redisKey = RedisKeyUtil.getCodeKey(email);
        String code = CommunityUtil.generationUUID().substring(0, 4);
        redisTemplate.opsForValue().set(redisKey, code, 60, TimeUnit.SECONDS);

        userService.sendForgetEmail(email, code);

        result.put("code", 0);
        return result;
    }
}

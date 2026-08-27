package com.collaborative.sharing.controller;

import com.collaborative.sharing.service.SettingsService;
import com.collaborative.sharing.util.AccessControlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/settings")
public class SettingsController {
    
    @Autowired
    private SettingsService settingsService;
    
    @GetMapping
    public String settings(Model model, HttpSession session) {
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        AccessControlUtil.fillModel(model, session);
        
        // 获取当前背景图片
        String backgroundImage = settingsService.getBackgroundImage();
        model.addAttribute("backgroundImage", backgroundImage);
        
        return "settings";
    }
    
    @PostMapping("/change-password")
    @ResponseBody
    public Map<String, Object> changePassword(@RequestParam("oldPassword") String oldPassword,
                                              @RequestParam("newPassword") String newPassword,
                                              @RequestParam("confirmPassword") String confirmPassword,
                                              HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        String username = (String) session.getAttribute("currentUser");
        if (username == null) {
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            result.put("success", false);
            result.put("message", "两次输入的新密码不一致");
            return result;
        }
        
        boolean success = settingsService.changePassword(username, oldPassword, newPassword);
        if (success) {
            result.put("success", true);
            result.put("message", "密码修改成功");
        } else {
            result.put("success", false);
            result.put("message", "旧密码错误");
        }
        
        return result;
    }
    
    @PostMapping("/upload-background")
    public String uploadBackground(@RequestParam("backgroundImage") MultipartFile file,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            String currentUser = (String) session.getAttribute("currentUser");
            if (currentUser == null) {
                return "redirect:/login";
            }
            if (AccessControlUtil.isExternalDepartment(session)) {
                redirectAttributes.addFlashAttribute("error", "外部部门账号无权修改系统背景");
                return "redirect:/settings";
            }
            
            settingsService.uploadBackgroundImage(file);
            redirectAttributes.addFlashAttribute("success", "背景图片上传成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "背景图片上传失败：" + e.getMessage());
        }
        
        return "redirect:/settings";
    }
}


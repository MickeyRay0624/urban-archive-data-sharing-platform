package com.collaborative.sharing.controller;

import com.collaborative.sharing.entity.ServiceComponent;
import com.collaborative.sharing.entity.TodoItem;
import com.collaborative.sharing.entity.User;
import com.collaborative.sharing.service.LoginService;
import com.collaborative.sharing.service.TodoItemService;
import com.collaborative.sharing.service.SettingsService;
import com.collaborative.sharing.mapper.ServiceComponentMapper;
import com.collaborative.sharing.util.AccessControlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class HomeController {

    private static final LocalDateTime SYSTEM_START_TIME = LocalDateTime.now();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ServiceComponentMapper serviceComponentMapper;

    @Autowired
    private TodoItemService todoItemService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private SettingsService settingsService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        // 检查登录状态
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (AccessControlUtil.isExternalDepartment(session)) {
            return "redirect:/service-components";
        }
        AccessControlUtil.fillModel(model, session);

        // 获取待办事项
        List<TodoItem> todoItems = todoItemService.findAll();
        model.addAttribute("todoItems", todoItems);

        // 获取当前日期信息
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");
        String currentDate = now.format(formatter);
        model.addAttribute("currentDate", currentDate);

        return "home";
    }

    @GetMapping("/login")
    public String login(Model model) {
        // 获取当前背景图片
        String backgroundImage = settingsService.getBackgroundImage();
        model.addAttribute("backgroundImage", backgroundImage);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        User user = loginService.login(username, password);
        if (user != null) {
            session.setAttribute("currentUser", username);
            session.setAttribute("realName", user.getRealName());
            String userRole = AccessControlUtil.normalizeRole(username, user.getUserRole());
            String department = user.getDepartment();
            if (department == null || department.trim().isEmpty()) {
                department = AccessControlUtil.ROLE_EXTERNAL_DEPARTMENT.equals(userRole)
                        ? user.getRealName()
                        : AccessControlUtil.ARCHIVE_DEPARTMENT;
            }
            session.setAttribute("department", department);
            session.setAttribute("userRole", userRole);
            session.setAttribute("loginTime", LocalDateTime.now());
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("error", "用户名或密码错误");
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/todo-items")
    public String todoItems(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) String processName,
                           @RequestParam(required = false) String uploader,
                           @RequestParam(required = false) String department,
                           @RequestParam(required = false) String status,
                           Model model, HttpSession session) {
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (AccessControlUtil.isExternalDepartment(session)) {
            return "redirect:/service-components";
        }
        AccessControlUtil.fillModel(model, session);

        // 使用分页查询
        com.github.pagehelper.PageInfo<TodoItem> pageInfo =
            todoItemService.findByPage(pageNum, pageSize, processName, uploader, department, status);

        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("processName", processName);
        model.addAttribute("uploader", uploader);
        model.addAttribute("department", department);
        model.addAttribute("status", status);

        return "todo-items";
    }

    @PostMapping("/todo-items/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteTodoItem(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String currentUser = (String) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "未登录");
                return result;
            }
            if (AccessControlUtil.isExternalDepartment(session)) {
                result.put("success", false);
                result.put("message", "外部部门账号无权访问审批管理");
                return result;
            }

            todoItemService.deleteTodoItem(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/todo-items/detail/{id}")
    public String todoItemDetail(@PathVariable Long id, Model model, HttpSession session) {
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (AccessControlUtil.isExternalDepartment(session)) {
            return "redirect:/service-components";
        }
        AccessControlUtil.fillModel(model, session);

        TodoItem todoItem = todoItemService.findById(id);
        model.addAttribute("todoItem", todoItem);
        return "todo-detail";
    }

    @PostMapping("/todo-items/create")
    public String createTodoItem(@RequestParam("processName") String processName,
                                  @RequestParam("department") String department,
                                  @RequestParam("processPurpose") String processPurpose,
                                  @RequestParam(value = "attachment", required = false) MultipartFile attachment,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            String uploader = (String) session.getAttribute("currentUser");
            if (uploader == null) {
                return "redirect:/login";
            }
            if (AccessControlUtil.isExternalDepartment(session)) {
                redirectAttributes.addFlashAttribute("error", "外部部门账号无权新增审批");
                return "redirect:/service-components";
            }

            todoItemService.createTodoItem(uploader, department, processName, processPurpose, attachment);
            redirectAttributes.addFlashAttribute("success", "流程提交成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "流程提交失败：" + e.getMessage());
        }
        return "redirect:/todo-items";
    }

    @PostMapping("/todo-items/review")
    @ResponseBody
    public Map<String, Object> reviewTodoItem(@RequestParam("id") Long id,
                                               @RequestParam("status") String status,
                                               @RequestParam("comment") String comment,
                                               HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String reviewer = (String) session.getAttribute("currentUser");
            if (reviewer == null) {
                result.put("success", false);
                result.put("message", "未登录");
                return result;
            }
            if (AccessControlUtil.isExternalDepartment(session)) {
                result.put("success", false);
                result.put("message", "外部部门账号无权审批");
                return result;
            }

            todoItemService.reviewTodoItem(id, reviewer, status, comment);
            result.put("success", true);
            result.put("message", "审批成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "审批失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/service-components")
    public String serviceComponents(Model model, HttpSession session) {
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        AccessControlUtil.fillModel(model, session);

        List<ServiceComponent> serviceComponents = serviceComponentMapper.findAll();
        if (AccessControlUtil.isExternalDepartment(session)) {
            String department = (String) session.getAttribute("department");
            List<ServiceComponent> visibleComponents = new ArrayList<>();
            for (ServiceComponent component : serviceComponents) {
                String status = component.getStatus() != null ? component.getStatus().name() : "";
                boolean ownedByCurrentDepartment = department != null && department.equals(component.getOwnerDepartment());
                boolean published = "PUBLISHED".equals(status);
                boolean authorizedForCurrentDepartment = "AUTHORIZED".equals(status)
                        && component.getAuthorizedUnits() != null
                        && department != null
                        && component.getAuthorizedUnits().contains(department);
                if (ownedByCurrentDepartment || published || authorizedForCurrentDepartment) {
                    visibleComponents.add(component);
                }
            }
            serviceComponents = visibleComponents;
        }
        model.addAttribute("serviceComponents", serviceComponents);
        return "service-components";
    }

    @GetMapping("/monitoring")
    public String monitoring(HttpSession session, Model model) {
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (AccessControlUtil.isExternalDepartment(session)) {
            return "redirect:/service-components";
        }
        AccessControlUtil.fillModel(model, session);
        populateMonitoringModel(model, session, currentUser);
        return "monitoring";
    }

    @GetMapping("/security")
    public String security(HttpSession session, Model model) {
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (AccessControlUtil.isExternalDepartment(session)) {
            return "redirect:/service-components";
        }
        AccessControlUtil.fillModel(model, session);
        populateSecurityModel(model, currentUser);
        return "security";
    }

    private void populateMonitoringModel(Model model, HttpSession session, String currentUser) {
        int todayCallCount = ThreadLocalRandom.current().nextInt(30, 51);
        model.addAttribute("todayCallCount", todayCallCount);
        model.addAttribute("todaySecurityEventCount", 2);
        model.addAttribute("callTrendTimes", buildWorkHourLabels());
        model.addAttribute("callTrendValues", distributeCallsAcrossWorkHours(todayCallCount));
        model.addAttribute("responseTrendValues", buildResponseTrendValues());
        model.addAttribute("monitoringEvents", buildMonitoringEvents(session, currentUser));
    }

    private void populateSecurityModel(Model model, String currentUser) {
        model.addAttribute("securityEvents", buildSecurityEvents(currentUser));
        model.addAttribute("securityTrendDates", buildRecentDateLabels(7));
        model.addAttribute("securityTrendValues", Arrays.asList(0, 1, 1, 0, 1, 2, 2));
        model.addAttribute("threatTrendValues", Arrays.asList(0, 0, 1, 0, 1, 1, 1));
    }

    private List<String> buildWorkHourLabels() {
        List<String> labels = new ArrayList<>();
        for (int hour = 8; hour <= 18; hour++) {
            labels.add(String.format("%02d:00", hour));
        }
        return labels;
    }

    private List<Integer> distributeCallsAcrossWorkHours(int total) {
        List<Integer> values = new ArrayList<>();
        int slots = 11;
        int remaining = total;
        for (int i = 0; i < slots; i++) {
            int slotsLeft = slots - i - 1;
            if (slotsLeft == 0) {
                values.add(remaining);
                break;
            }
            int maxForSlot = Math.min(7, remaining - slotsLeft);
            int value = ThreadLocalRandom.current().nextInt(1, maxForSlot + 1);
            values.add(value);
            remaining -= value;
        }
        return values;
    }

    private List<Integer> buildResponseTrendValues() {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            values.add(ThreadLocalRandom.current().nextInt(36, 56));
        }
        return values;
    }

    private List<Map<String, Object>> buildMonitoringEvents(HttpSession session, String currentUser) {
        LocalDateTime now = LocalDateTime.now();
        Object loginTimeValue = session.getAttribute("loginTime");
        LocalDateTime loginTime = loginTimeValue instanceof LocalDateTime
                ? (LocalDateTime) loginTimeValue
                : now;
        if (!(loginTimeValue instanceof LocalDateTime)) {
            session.setAttribute("loginTime", loginTime);
        }

        String realName = (String) session.getAttribute("realName");
        String displayName = realName != null && !realName.trim().isEmpty() ? realName : currentUser;

        List<Map<String, Object>> events = new ArrayList<>();
        events.add(monitoringEvent(now, "访问", "访问监控运维", displayName + "访问监控运维模块，查看服务调用趋势和实时事件日志", "正常", "green"));
        events.add(monitoringEvent(now.minusSeconds(12), "操作", "刷新监控指标", displayName + "刷新今日调用次数、服务状态和运行日志", "正常", "green"));
        events.add(monitoringEvent(loginTime, "认证", "用户登录", displayName + "登录系统，账号：" + currentUser, "正常", "green"));
        events.add(monitoringEvent(SYSTEM_START_TIME, "系统", "系统启动", "协同共享服务系统启动完成，Web 服务进入运行状态", "正常", "green"));
        return events;
    }

    private Map<String, Object> monitoringEvent(LocalDateTime time, String type, String name,
                                                String description, String status, String statusClass) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("time", time.format(DATE_TIME_FORMATTER));
        event.put("type", type);
        event.put("name", name);
        event.put("description", description);
        event.put("status", status);
        event.put("statusClass", statusClass);
        return event;
    }

    private List<Map<String, Object>> buildSecurityEvents(String currentUser) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        List<Map<String, Object>> events = new ArrayList<>();
        events.add(securityEvent(now.minusMinutes(18), "登录成功", "192.168.1.100",
                "用户 " + currentUser + " 登录系统并通过身份认证", "低", "severity-low", "已处理", "status-processed"));
        events.add(securityEvent(now.minusHours(2), "权限验证", "192.168.1.100",
                "管理员访问安全防护模块，系统完成角色权限校验", "低", "severity-low", "已记录", "status-recorded"));
        events.add(securityEvent(today.minusDays(1).atTime(16, 35, 18), "数据导出审计", "192.168.1.100",
                "业务元数据字段清单导出请求已纳入审计日志", "中", "severity-medium", "已记录", "status-recorded"));
        events.add(securityEvent(today.minusDays(1).atTime(10, 18, 42), "异常访问", "192.168.1.200",
                "检测到未授权路径访问尝试，网关已拦截", "中", "severity-medium", "已阻止", "status-blocked"));
        events.add(securityEvent(today.minusDays(2).atTime(15, 42, 9), "系统加固", "127.0.0.1",
                "系统安全策略和审计日志规则检查完成", "低", "severity-low", "已处理", "status-processed"));
        return events;
    }

    private Map<String, Object> securityEvent(LocalDateTime time, String type, String sourceIp, String description,
                                              String severity, String severityClass, String status, String statusClass) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("time", time.format(DATE_TIME_FORMATTER));
        event.put("type", type);
        event.put("sourceIp", sourceIp);
        event.put("description", description);
        event.put("severity", severity);
        event.put("severityClass", severityClass);
        event.put("status", status);
        event.put("statusClass", statusClass);
        return event;
    }

    private List<String> buildRecentDateLabels(int days) {
        List<String> labels = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            labels.add(today.minusDays(i).format(formatter));
        }
        return labels;
    }

    // 下载审批附件
    @GetMapping("/todo-items/download/{id}")
    public ResponseEntity<byte[]> downloadTodoAttachment(@PathVariable Long id, HttpSession session) {
        try {
            String currentUser = (String) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(401).build();
            }
            if (AccessControlUtil.isExternalDepartment(session)) {
                return ResponseEntity.status(403).build();
            }

            TodoItem todoItem = todoItemService.findById(id);
            if (todoItem == null || todoItem.getAttachmentPath() == null) {
                return ResponseEntity.notFound().build();
            }

            // 使用FileUploadUtil获取绝对路径
            String absolutePath = com.collaborative.sharing.util.FileUploadUtil.getAbsolutePath(todoItem.getAttachmentPath());
            java.io.File file = new java.io.File(absolutePath);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", todoItem.getAttachmentName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

}

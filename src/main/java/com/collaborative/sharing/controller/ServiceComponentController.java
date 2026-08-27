package com.collaborative.sharing.controller;

import com.collaborative.sharing.entity.ServiceComponent;
import com.collaborative.sharing.service.ServiceComponentService;
import com.collaborative.sharing.util.AccessControlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/service-components")
public class ServiceComponentController {

    @Autowired
    private ServiceComponentService serviceComponentService;

    // ========== 基础 CRUD ==========

    @ResponseBody
    @GetMapping("/api/{id}")
    public Map<String, Object> getServiceJson(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }
        ServiceComponent service = serviceComponentService.findById(id);
        if (service == null) {
            result.put("success", false);
            result.put("message", "服务不存在");
            return result;
        }
        if (AccessControlUtil.isExternalDepartment(session) && !canViewService(service, session)) {
            result.put("success", false);
            result.put("message", "无权查看该服务");
            return result;
        }
        result.put("id", service.getId());
        result.put("serviceName", service.getServiceName());
        result.put("serviceCode", service.getServiceCode());
        result.put("description", service.getDescription());
        result.put("version", service.getVersion());
        result.put("apiUrl", service.getApiUrl());
        result.put("ownerDepartment", service.getOwnerDepartment());
        result.put("securityLevel", service.getSecurityLevel());
        result.put("authorizedUnits", service.getAuthorizedUnits());
        result.put("configJson", service.getConfigJson());
        result.put("remark", service.getRemark());
        result.put("status", service.getStatus() != null ? service.getStatus().name() : "");
        result.put("success", true);
        return result;
    }

    @ResponseBody
    @PostMapping("/create")
    public Map<String, Object> create(@RequestParam("serviceName") String serviceName,
                                      @RequestParam("serviceCode") String serviceCode,
                                      @RequestParam("description") String description,
                                      @RequestParam("version") String version,
                                      @RequestParam(value = "apiUrl", required = false) String apiUrl,
                                      @RequestParam(value = "ownerDepartment", required = false) String ownerDepartment,
                                      @RequestParam(value = "securityLevel", required = false) String securityLevel,
                                      @RequestParam(value = "authorizedUnits", required = false) String authorizedUnits,
                                      @RequestParam(value = "configJson", required = false) String configJson,
                                      @RequestParam(value = "remark", required = false) String remark,
                                      HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String currentUser = (String) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "未登录");
                return result;
            }
            if (AccessControlUtil.isExternalDepartment(session)) {
                ownerDepartment = currentDepartment(session);
                authorizedUnits = null;
            }

            ServiceComponent component = new ServiceComponent();
            component.setServiceName(serviceName);
            component.setServiceCode(serviceCode);
            component.setDescription(description);
            component.setVersion(version);
            component.setApiUrl(apiUrl);
            component.setOwnerDepartment(ownerDepartment);
            component.setSecurityLevel(securityLevel);
            component.setAuthorizedUnits(authorizedUnits);
            component.setConfigJson(configJson);
            component.setRemark(remark);
            component.setStatus(ServiceComponent.ServiceStatus.DRAFT);

            serviceComponentService.create(component);
            result.put("success", true);
            result.put("message", "服务创建成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "创建失败：" + e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @PostMapping("/update")
    public Map<String, Object> update(@RequestParam("id") Long id,
                                      @RequestParam("serviceName") String serviceName,
                                      @RequestParam("serviceCode") String serviceCode,
                                      @RequestParam("description") String description,
                                      @RequestParam("version") String version,
                                      @RequestParam(value = "apiUrl", required = false) String apiUrl,
                                      @RequestParam(value = "ownerDepartment", required = false) String ownerDepartment,
                                      @RequestParam(value = "securityLevel", required = false) String securityLevel,
                                      @RequestParam(value = "authorizedUnits", required = false) String authorizedUnits,
                                      @RequestParam(value = "configJson", required = false) String configJson,
                                      @RequestParam(value = "remark", required = false) String remark,
                                      HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String currentUser = (String) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "未登录");
                return result;
            }

            ServiceComponent existing = serviceComponentService.findById(id);
            if (existing == null) {
                result.put("success", false);
                result.put("message", "服务组件不存在");
                return result;
            }
            if (AccessControlUtil.isExternalDepartment(session) && !isOwnedByCurrentDepartment(existing, session)) {
                result.put("success", false);
                result.put("message", "只能编辑本部门注册的服务");
                return result;
            }
            if (AccessControlUtil.isExternalDepartment(session)) {
                ownerDepartment = currentDepartment(session);
                authorizedUnits = existing.getAuthorizedUnits();
            }

            existing.setServiceName(serviceName);
            existing.setServiceCode(serviceCode);
            existing.setDescription(description);
            existing.setVersion(version);
            existing.setApiUrl(apiUrl);
            existing.setOwnerDepartment(ownerDepartment);
            existing.setSecurityLevel(securityLevel);
            existing.setAuthorizedUnits(authorizedUnits);
            existing.setConfigJson(configJson != null ? configJson : existing.getConfigJson());
            existing.setRemark(remark);

            serviceComponentService.update(existing);
            result.put("success", true);
            result.put("message", "更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @PostMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String currentUser = (String) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "未登录");
                return result;
            }
            ServiceComponent existing = serviceComponentService.findById(id);
            if (existing == null) {
                result.put("success", false);
                result.put("message", "服务组件不存在");
                return result;
            }
            if (AccessControlUtil.isExternalDepartment(session)) {
                if (!isOwnedByCurrentDepartment(existing, session)) {
                    result.put("success", false);
                    result.put("message", "只能删除本部门注册的服务");
                    return result;
                }
                String status = existing.getStatus() != null ? existing.getStatus().name() : "";
                if (!"DRAFT".equals(status) && !"OFFLINE".equals(status)) {
                    result.put("success", false);
                    result.put("message", "已提交或已发布的服务不能由外部部门直接删除");
                    return result;
                }
            }
            serviceComponentService.delete(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @PostMapping("/submit/{id}")
    public Map<String, Object> submit(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            String currentUser = (String) session.getAttribute("currentUser");
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "未登录");
                return result;
            }
            ServiceComponent existing = serviceComponentService.findById(id);
            if (existing == null) {
                result.put("success", false);
                result.put("message", "服务组件不存在");
                return result;
            }
            if (AccessControlUtil.isExternalDepartment(session) && !isOwnedByCurrentDepartment(existing, session)) {
                result.put("success", false);
                result.put("message", "只能提交本部门注册的服务");
                return result;
            }
            serviceComponentService.updateStatus(id, "PENDING_APPROVAL");
            result.put("success", true);
            result.put("message", "已提交审批");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败：" + e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @PostMapping("/publish/{id}")
    public Map<String, Object> publish(@PathVariable Long id, HttpSession session) {
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
                result.put("message", "外部部门账号无权发布服务");
                return result;
            }
            serviceComponentService.updateStatus(id, "PUBLISHED");
            result.put("success", true);
            result.put("message", "已发布服务");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败：" + e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @PostMapping("/offline/{id}")
    public Map<String, Object> offline(@PathVariable Long id, HttpSession session) {
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
                result.put("message", "外部部门账号无权下架服务");
                return result;
            }
            serviceComponentService.updateStatus(id, "OFFLINE");
            result.put("success", true);
            result.put("message", "已下架服务");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败：" + e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @PostMapping("/authorize/{id}")
    public Map<String, Object> authorize(@PathVariable Long id,
                                          @RequestParam("authorizedUnits") String authorizedUnits,
                                          HttpSession session) {
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
                result.put("message", "外部部门账号无权授权服务");
                return result;
            }
            serviceComponentService.updateAuthorization(id, authorizedUnits);
            result.put("success", true);
            result.put("message", "授权成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "授权失败：" + e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @PostMapping("/config/{id}")
    public Map<String, Object> config(@PathVariable Long id,
                                       @RequestParam("configJson") String configJson,
                                       HttpSession session) {
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
                result.put("message", "外部部门账号无权配置服务");
                return result;
            }
            serviceComponentService.updateConfig(id, configJson);
            result.put("success", true);
            result.put("message", "配置保存成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "配置保存失败：" + e.getMessage());
        }
        return result;
    }

    private String currentDepartment(HttpSession session) {
        Object value = session.getAttribute("department");
        return value != null ? value.toString() : "";
    }

    private boolean isOwnedByCurrentDepartment(ServiceComponent service, HttpSession session) {
        String currentDepartment = currentDepartment(session);
        return currentDepartment != null
                && service.getOwnerDepartment() != null
                && currentDepartment.equals(service.getOwnerDepartment());
    }

    private boolean canViewService(ServiceComponent service, HttpSession session) {
        if (isOwnedByCurrentDepartment(service, session)) {
            return true;
        }
        String status = service.getStatus() != null ? service.getStatus().name() : "";
        if ("PUBLISHED".equals(status)) {
            return true;
        }
        String currentDepartment = currentDepartment(session);
        return "AUTHORIZED".equals(status)
                && service.getAuthorizedUnits() != null
                && currentDepartment != null
                && service.getAuthorizedUnits().contains(currentDepartment);
    }

}

package com.collaborative.sharing.util;

import org.springframework.ui.Model;

import javax.servlet.http.HttpSession;

public class AccessControlUtil {
    public static final String ROLE_ARCHIVE_ADMIN = "ARCHIVE_ADMIN";
    public static final String ROLE_EXTERNAL_DEPARTMENT = "EXTERNAL_DEPARTMENT";
    public static final String ARCHIVE_DEPARTMENT = "城建档案局";

    private AccessControlUtil() {
    }

    public static boolean isExternalDepartment(HttpSession session) {
        return ROLE_EXTERNAL_DEPARTMENT.equals(session.getAttribute("userRole"));
    }

    public static boolean isArchiveAdmin(HttpSession session) {
        return !isExternalDepartment(session);
    }

    public static String normalizeRole(String username, String role) {
        if (role != null && !role.trim().isEmpty()) {
            return role;
        }
        return "admin".equals(username) ? ROLE_ARCHIVE_ADMIN : ROLE_EXTERNAL_DEPARTMENT;
    }

    public static void fillModel(Model model, HttpSession session) {
        model.addAttribute("currentUser", session.getAttribute("currentUser"));
        model.addAttribute("realName", session.getAttribute("realName"));
        model.addAttribute("currentDepartment", session.getAttribute("department"));
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("isExternalDepartment", isExternalDepartment(session));
        model.addAttribute("isArchiveAdmin", isArchiveAdmin(session));
    }

}

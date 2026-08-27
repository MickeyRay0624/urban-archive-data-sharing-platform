package com.collaborative.sharing.service;

import com.collaborative.sharing.entity.TodoItem;
import com.collaborative.sharing.mapper.TodoItemMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.collaborative.sharing.util.FileUploadUtil;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TodoItemService {
    
    @Autowired
    private TodoItemMapper todoItemMapper;
    
    /**
     * 获取所有待办事项
     */
    public List<TodoItem> findAll() {
        return todoItemMapper.findAll();
    }
    
    /**
     * 分页查询待办事项
     */
    public PageInfo<TodoItem> findByPage(int pageNum, int pageSize, String processName, 
                                         String uploader, String department, String status) {
        PageHelper.startPage(pageNum, pageSize);
        List<TodoItem> list = todoItemMapper.findByConditions(processName, uploader, department, status);
        return new PageInfo<>(list);
    }
    
    /**
     * 根据ID查找待办事项
     */
    public TodoItem findById(Long id) {
        return todoItemMapper.findById(id);
    }
    
    /**
     * 创建新的待办事项（上传流程）
     */
    public void createTodoItem(String uploader, String department, String processName, 
                               String processPurpose, MultipartFile attachment) throws Exception {
        TodoItem todoItem = new TodoItem();
        todoItem.setUploader(uploader);
        todoItem.setDepartment(department);
        todoItem.setProcessName(processName);
        todoItem.setProcessPurpose(processPurpose);
        todoItem.setStatus(TodoItem.TodoStatus.PENDING);
        todoItem.setUploadTime(LocalDateTime.now());
        todoItem.setCreatedAt(LocalDateTime.now());
        todoItem.setUpdatedAt(LocalDateTime.now());
        
        // 处理附件上传
        if (attachment != null && !attachment.isEmpty()) {
            String filePath = FileUploadUtil.saveFile(attachment, "todo");
            todoItem.setAttachmentPath(filePath);
            todoItem.setAttachmentName(attachment.getOriginalFilename());
        }
        
        todoItemMapper.insert(todoItem);
    }
    
    /**
     * 审批待办事项
     */
    public void reviewTodoItem(Long id, String reviewer, String status, String comment) {
        TodoItem todoItem = todoItemMapper.findById(id);
        if (todoItem == null) {
            throw new RuntimeException("待办事项不存在");
        }
        
        todoItem.setReviewer(reviewer);
        todoItem.setReviewTime(LocalDateTime.now());
        todoItem.setReviewComment(comment);
        
        if ("approve".equals(status)) {
            todoItem.setStatus(TodoItem.TodoStatus.APPROVED);
        } else if ("reject".equals(status)) {
            todoItem.setStatus(TodoItem.TodoStatus.REJECTED);
        }
        
        todoItem.setUpdatedAt(LocalDateTime.now());
        todoItemMapper.update(todoItem);
    }
    
    /**
     * 删除待办事项
     */
    public void deleteTodoItem(Long id) {
        TodoItem todoItem = todoItemMapper.findById(id);
        if (todoItem != null && todoItem.getAttachmentPath() != null) {
            // 删除关联的附件
            FileUploadUtil.deleteFile(todoItem.getAttachmentPath());
        }
        todoItemMapper.deleteById(id);
    }
}


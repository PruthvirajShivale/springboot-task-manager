package com.example.demo.controller;

import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.repository.TaskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/";
        
        model.addAttribute("tasks", taskRepository.findByUserId(user.getId()));
        model.addAttribute("username", user.getUsername());
        return "dashboard";
    }

    @PostMapping("/task/save")
    public String saveTask(@RequestParam(required = false) Long id,
                           @RequestParam String title,
                           @RequestParam String description,
                           @RequestParam("file") MultipartFile file,
                           HttpSession session) throws IOException {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/";

        Task task = new Task();
        if (id != null) {
            task = taskRepository.findById(id).orElse(new Task());
        }
        
        task.setTitle(title);
        task.setDescription(description);
        task.setUserId(user.getId());

        if (!file.isEmpty()) {
            task.setFileName(file.getOriginalFilename());
            task.setFileData(file.getBytes());
        } else if (id != null) {
            // Agar edit kar rahe hain aur nayi file upload nahi ki, to purani file retain rakhein
            Task oldTask = taskRepository.findById(id).orElse(null);
            if (oldTask != null) {
                task.setFileName(oldTask.getFileName());
                task.setFileData(oldTask.getFileData());
            }
        }

        taskRepository.save(task);
        return "redirect:/dashboard";
    }

    @GetMapping("/task/edit/{id}")
    public String editTask(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) return "redirect:/";
        Task task = taskRepository.findById(id).orElse(null);
        model.addAttribute("task", task);
        return "edit-task";
    }

    @GetMapping("/task/delete/{id}")
    public String deleteTask(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) return "redirect:/";
        taskRepository.deleteById(id);
        return "redirect:/dashboard";
    }

    @GetMapping("/task/download/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null || task.getFileData() == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + task.getFileName() + "\"")
                .body(task.getFileData());
    }
}
package protopnet.mlprototypesfeedbackcollector.controllers;

import  protopnet.mlprototypesfeedbackcollector.model.User;
import protopnet.mlprototypesfeedbackcollector.service.UserService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/register")
public class RegisterController {

    private final UserService userService;

    @Autowired
    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String showRegistrationForm() {
        return "Register";
    }

    @PostMapping
    public String registerUser(User user) {
        User existingUser = userService.findByUsername(user.getUsername());
        if (existingUser != null) {
            return "redirect:/register?usernameTaken";
        }


        userService.registerUser(user);
        return "redirect:/login?regisrationSuccess";
    }

}


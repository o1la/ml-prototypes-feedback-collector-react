package protopnet.mlprototypesfeedbackcollector.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {

    @GetMapping("/")
    public String showHomePage(Model model) {
        return "HomePage";
    }

    @GetMapping("/home")
    public String showHomePageForRegisteredUser(Model model) {
        return "HomePageForRegisteredUser";
    }

    @GetMapping("/about-us")
    public String showAboutUs(Model model){return "AboutUs";}
}

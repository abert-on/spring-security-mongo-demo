package security.controller;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import security.domain.User;
import security.service.CustomerUserDetailsService;
import security.service.EmailService;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class UserDetailsController {

    @Autowired
    private CustomerUserDetailsService userDetailsService;

    @Autowired
    private EmailService mailService;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @GetMapping("/changepassword")
    public ModelAndView showChangePasswordPage(final ModelAndView modelAndView, final User user) {
        modelAndView.addObject("user", user);
        modelAndView.setViewName("changepassword");
        return modelAndView;
    }

    @PostMapping("/changepassword")
    public ModelAndView processChangePasswordForm(final ModelAndView modelAndView, final BindingResult bindingResult, @RequestParam final Map<String, String> requestParams, final RedirectAttributes redir, final HttpServletRequest request) {

        if (!requestParams.get("password").equals(requestParams.get("confirmpassword"))) {
            bindingResult.reject("confirmPassword");
            redir.addFlashAttribute("errorMessage", "Passwords do not match.");

            modelAndView.setViewName("redirect:changepassword");
            return modelAndView;
        }

        final Zxcvbn passwordCheck = new Zxcvbn();

        final Strength strength = passwordCheck.measure(requestParams.get("password"));

        if (strength.getScore() < 3) {
            bindingResult.reject("password");
            redir.addFlashAttribute("errorMessage", "Your password is too weak. Choose a stronger one.");

            modelAndView.setViewName("redirect:confirm?token=" + requestParams.get("token"));
            return modelAndView;
        }

        final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!this.encoder.matches(requestParams.get("oldPassword"), user.getPassword())) {
            bindingResult.reject("oldPassword");
            redir.addFlashAttribute("errorMessage", "Old password is incorrect.");

            modelAndView.setViewName("redirect:changepassword");
            return modelAndView;
        }

        user.setPassword(this.encoder.encode(requestParams.get("password")));
        this.userDetailsService.save(user);

        final SimpleMailMessage confirmationEmail = new SimpleMailMessage();
        confirmationEmail.setTo(user.getUsername());
        confirmationEmail.setSubject("Your email has been successfully changed");
        confirmationEmail.setText("If this was not you then please contact support :\n"
                + "mail@alecburton.co.uk");
        confirmationEmail.setFrom("mail@alecburton.co.uk");

        mailService.sendEmail(confirmationEmail);

        modelAndView.addObject("successMessage", "Your password has been changed.");
        modelAndView.setViewName("success");
        return modelAndView;
    }
}

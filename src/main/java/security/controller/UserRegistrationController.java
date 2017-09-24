package security.controller;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
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
import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Controller
public class UserRegistrationController {

    @Autowired
    private CustomerUserDetailsService userDetailsService;

    @Autowired
    private EmailService mailService;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @GetMapping("/registration")
    public ModelAndView showRegistrationPage(final ModelAndView modelAndView, final User user) {
        modelAndView.addObject("user", user);
        modelAndView.setViewName("registration");
        return modelAndView;
    }

    @PostMapping("/registration")
    public ModelAndView processRegistrationForm(final ModelAndView modelAndView, @Valid final User user, final BindingResult bindingResult, final HttpServletRequest request) {
        if (this.userDetailsService.emailExists(user.getUsername())) {
            modelAndView.addObject("alreadyRegisteredMessage", "Email address " + user.getUsername() + " is already registered.");
            bindingResult.reject("email");
        }

        if (bindingResult.hasErrors()) {
            modelAndView.setViewName("registration");
        }
        else {
            // disable user and set a confirmation token
            user.setEnabled(false);
            user.setGrantedAuthorities(Collections.singletonList("USER"));
            user.setConfirmationToken(UUID.randomUUID().toString());
            this.userDetailsService.save(user);

            final String appUrl = request.getScheme() + "://" + request.getServerName();

            final SimpleMailMessage registrationEmail = new SimpleMailMessage();
            registrationEmail.setTo(user.getUsername());
            registrationEmail.setSubject("Registration Confirmation");
            registrationEmail.setText("To confirm your e-mail address, please click the link below:\n"
                    + appUrl + "/confirm?token=" + user.getConfirmationToken());
            registrationEmail.setFrom("mail@alecburton.co.uk");

            mailService.sendEmail(registrationEmail);

            modelAndView.addObject("confirmationMessage", "A confirmation e-mail has been sent to " + user.getUsername());
            modelAndView.setViewName("registration");
        }
        return modelAndView;
    }

    @GetMapping("/confirm")
    public ModelAndView showConfirmationPage(final ModelAndView modelAndView, @RequestParam("token") final String token) {
        final User user = this.userDetailsService.findByConfirmationToken(token);

        if (user == null) {
            modelAndView.addObject("invalidToken", "This is an invalid confirmation link.");
        }
        else {
            modelAndView.addObject("confirmationToken", user.getConfirmationToken());
        }

        modelAndView.setViewName("confirm");
        return modelAndView;
    }

    @PostMapping("/confirm")
    public ModelAndView processConfirmation(final ModelAndView modelAndView, final BindingResult bindingResult, @RequestParam final Map<String, String> requestParams, final RedirectAttributes redir) {
        final Zxcvbn passwordCheck = new Zxcvbn();

        final Strength strength = passwordCheck.measure(requestParams.get("password"));

        if (strength.getScore() < 3) {
            bindingResult.reject("password");
            redir.addFlashAttribute("errorMessage", "Your password is too weak. Choose a stronger one.");

            modelAndView.setViewName("redirect:confirm?token=" + requestParams.get("token"));
            return modelAndView;
        }

        final User user = userDetailsService.findByConfirmationToken(requestParams.get("token"));
        user.setPassword(this.encoder.encode(requestParams.get("password")));
        user.setEnabled(true);
        user.setConfirmationToken("");

        this.userDetailsService.save(user);

        modelAndView.addObject("successMessage", "Your password has been set.");
        modelAndView.setViewName("login");
        return modelAndView;
    }

    @GetMapping("/reset")
    public ModelAndView showResetPage(final ModelAndView modelAndView, final User user) {
        modelAndView.addObject("user", user);
        modelAndView.setViewName("reset");
        return modelAndView;
    }

    @PostMapping("/reset")
    public ModelAndView processResetForm(final ModelAndView modelAndView, @RequestParam final String email, final BindingResult bindingResult, final HttpServletRequest request) {
        final SimpleMailMessage resetEmail = new SimpleMailMessage();
        resetEmail.setTo(email);
        resetEmail.setFrom("mail@alecburton.co.uk");
        resetEmail.setSubject("Password Reset");

        if (this.userDetailsService.emailExists(email)) {
            final User user = (User) this.userDetailsService.loadUserByUsername(email);
            user.setEnabled(false);
            user.setPassword("");
            user.setConfirmationToken(UUID.randomUUID().toString());
            this.userDetailsService.save(user);

            final String appUrl = request.getScheme() + "://" + request.getServerName();

            resetEmail.setText("Click the link below to reset your password:\n"
                    + appUrl + "/confirm?token=" + user.getConfirmationToken());

        }
        else {
            final String appUrl = request.getScheme() + "://" + request.getServerName();
            resetEmail.setText("A password reset for was requested for " + email + " but no account with this email was found. Click the link below to register:\n"
                    + appUrl + "/registration");
        }
        mailService.sendEmail(resetEmail);

        modelAndView.addObject("successMessage", "A reset password e-mail has been sent to " + email);
        modelAndView.setViewName("reset");

        return modelAndView;
    }
}

package com.harsh.urlshortner.controller;

import com.harsh.urlshortner.dto.CreateUrlRequest;
import com.harsh.urlshortner.dto.UrlResponse;
import com.harsh.urlshortner.exception.BadRequestException;
import com.harsh.urlshortner.service.UrlService;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class WebUrlController {

    private final UrlService urlService;

    public WebUrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        Long userId = Long.parseLong(authentication.getName());

        List<UrlResponse> urls =
                urlService.getUserUrls(userId);

        model.addAttribute("urls", urls);

        model.addAttribute(
                "createUrlRequest",
                new CreateUrlRequest()
        );

        return "dashboard";
    }

    @PostMapping("/dashboard")
    public String createUrl(
            @Valid @ModelAttribute("createUrlRequest")
            CreateUrlRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            Model model) {

        Long userId = Long.parseLong(authentication.getName());

        if (bindingResult.hasErrors()) {

            List<UrlResponse> urls =
                    urlService.getUserUrls(userId);

            model.addAttribute("urls", urls);

            return "dashboard";
        }

        try {
            urlService.createUrl(request, userId);
        } catch (BadRequestException ex) {

            bindingResult.reject(
                    "creationError",
                    ex.getMessage()
            );

            List<UrlResponse> urls =
                    urlService.getUserUrls(userId);

            model.addAttribute("urls", urls);

            return "dashboard";
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/{shortCode}/delete")
    public String deleteUrl(
            @PathVariable String shortCode,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());

        urlService.deleteUrl(shortCode, userId);

        return "redirect:/dashboard";
    }
}
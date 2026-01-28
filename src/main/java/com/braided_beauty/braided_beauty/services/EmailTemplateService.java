package com.braided_beauty.braided_beauty.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@AllArgsConstructor
public class EmailTemplateService {
    private final SpringTemplateEngine templateEngine;

    public String render(String templateName, Map<String, Object> model) {
        Context thymeleafContext = new Context();
        thymeleafContext.setVariables(model);
        return templateEngine.process("email/" + templateName, thymeleafContext);
    }
}

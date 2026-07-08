package com.aiflow.enterprise.notification.service;

import com.aiflow.enterprise.notification.entity.NotificationTemplate;
import com.aiflow.enterprise.notification.entity.embedded.LocalizedContent;
import com.aiflow.enterprise.notification.enums.NotificationType;
import com.aiflow.enterprise.notification.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationTemplateService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateService.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final NotificationTemplateRepository templateRepository;

    public NotificationTemplateService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public NotificationTemplate findTemplate(String templateIdOrName) {
        return templateRepository.findById(templateIdOrName)
                .orElseGet(() -> templateRepository.findByName(templateIdOrName).orElse(null));
    }

    public NotificationTemplate findTemplateByType(NotificationType type) {
        var templates = templateRepository.findByTypeAndEnabledTrue(type);
        return templates.isEmpty() ? null : templates.getFirst();
    }

    public String renderTemplate(String template, Map<String, Object> context) {
        if (template == null) return "";
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = resolveContextValue(key, context);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value != null ? value.toString() : ""));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public RenderedContent render(NotificationTemplate template, String locale,
                                   Map<String, Object> context) {
        Map<String, Object> mergedContext = template.getDefaultContext() != null
                ? new java.util.HashMap<>(template.getDefaultContext()) : new java.util.HashMap<>();
        if (context != null) mergedContext.putAll(context);

        LocalizedContent localized = resolveLocalization(template, locale);

        String subject = renderTemplate(
                localized != null && localized.getSubject() != null
                        ? localized.getSubject() : template.getSubjectTemplate(),
                mergedContext);
        String body = renderTemplate(
                localized != null && localized.getBody() != null
                        ? localized.getBody() : template.getBodyTemplate(),
                mergedContext);
        String htmlBody = renderTemplate(
                localized != null && localized.getHtmlBody() != null
                        ? localized.getHtmlBody() : template.getHtmlBodyTemplate(),
                mergedContext);
        String pushTitle = renderTemplate(
                localized != null && localized.getPushTitle() != null
                        ? localized.getPushTitle() : template.getPushTitleTemplate(),
                mergedContext);
        String pushBody = renderTemplate(
                localized != null && localized.getPushBody() != null
                        ? localized.getPushBody() : template.getPushBodyTemplate(),
                mergedContext);
        String smsBody = renderTemplate(
                localized != null && localized.getSmsBody() != null
                        ? localized.getSmsBody() : template.getSmsBodyTemplate(),
                mergedContext);

        return new RenderedContent(subject, body, htmlBody, pushTitle, pushBody, smsBody);
    }

    public Map<String, Object> validateContextKeys(NotificationTemplate template, Map<String, Object> context) {
        if (template.getRequiredContextKeys() == null || template.getRequiredContextKeys().isEmpty()) {
            return Map.of("valid", true);
        }
        var missing = template.getRequiredContextKeys().stream()
                .filter(k -> context == null || !context.containsKey(k))
                .toList();
        if (!missing.isEmpty()) {
            return Map.of("valid", false, "missingKeys", missing);
        }
        return Map.of("valid", true);
    }

    private LocalizedContent resolveLocalization(NotificationTemplate template, String locale) {
        if (locale == null || template.getLocalizedContents() == null) return null;
        return template.getLocalizedContents().stream()
                .filter(lc -> locale.equalsIgnoreCase(lc.getLocale()))
                .findFirst()
                .orElse(null);
    }

    private Object resolveContextValue(String key, Map<String, Object> context) {
        if (context == null) return null;
        if (key.contains(".")) {
            String[] parts = key.split("\\.", 2);
            Object nested = context.get(parts[0]);
            if (nested instanceof Map<?, ?> map) {
                return map.get(parts[1]);
            }
            return null;
        }
        return context.get(key);
    }

    public record RenderedContent(
            String subject, String body, String htmlBody,
            String pushTitle, String pushBody, String smsBody
    ) {}
}

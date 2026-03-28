package com.mohammadnuridin.todolistapp.core.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Resolve message key berdasarkan locale request saat ini.
     * Locale diambil otomatis dari Accept-Language header via LocaleContextHolder.
     */
    public String get(String key) {
        return messageSource.getMessage(
                key,
                null,
                key, // fallback: tampilkan key jika tidak ditemukan
                LocaleContextHolder.getLocale());
    }

    /**
     * Resolve message key dengan arguments (untuk {min}, {max}, dll).
     */
    public String get(String key, Object... args) {
        return messageSource.getMessage(
                key,
                args,
                key,
                LocaleContextHolder.getLocale());
    }
}
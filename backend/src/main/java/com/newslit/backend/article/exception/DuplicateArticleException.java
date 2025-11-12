package com.newslit.backend.article.exception;

public class DuplicateArticleException extends RuntimeException {
    public DuplicateArticleException(String message) {
        super(message);
    }
}

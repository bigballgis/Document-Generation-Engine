package com.bank.docgen.authorization.management.service;

/**
 * Renewal was requested after the session's absolute limit (default 8h from first login)
 * elapsed — the user must sign in again (401 SESSION_ABSOLUTE_LIMIT_REACHED).
 */
public class SessionAbsoluteLimitReachedException extends RuntimeException {
}

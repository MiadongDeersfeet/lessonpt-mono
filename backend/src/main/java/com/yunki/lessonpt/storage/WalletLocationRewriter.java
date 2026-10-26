package com.yunki.lessonpt.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wallet zip을 Windows에서 받으면 sqlnet.ora DIRECTORY가 C:/... 로 남는다.
 * JDBC는 따옴표를 경로 문자로 읽으므로 DIRECTORY=/var/lessonpt/wallet 형태로 맞춘다.
 * 인증서·키 파일은 건드리지 않는다.
 */
public final class WalletLocationRewriter {

    private static final Logger log = LoggerFactory.getLogger(WalletLocationRewriter.class);

    static final Pattern DIRECTORY = Pattern.compile(
            "(DIRECTORY\\s*=\\s*)\"?([^)\"]+)\"?",
            Pattern.CASE_INSENSITIVE);

    private static final String[] CONFIG_FILES = {"sqlnet.ora", "ojdbc.properties"};

    private WalletLocationRewriter() {
    }

    public static String normalizeWalletDir(String walletDir) {
        if (walletDir == null) {
            return null;
        }
        return stripQuotes(walletDir).replace('\\', '/');
    }

    public static String rewriteContent(String content, String walletDir) {
        if (content == null || walletDir == null || walletDir.isBlank()) {
            return content;
        }
        String normalized = normalizeWalletDir(walletDir);
        Matcher matcher = DIRECTORY.matcher(content);
        StringBuilder updated = new StringBuilder();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            matcher.appendReplacement(updated, Matcher.quoteReplacement(matcher.group(1) + normalized));
        }
        if (!found) {
            return content;
        }
        matcher.appendTail(updated);
        return updated.toString();
    }

    public static boolean hasQuotedDirectory(String content) {
        if (content == null) {
            return false;
        }
        Matcher matcher = DIRECTORY.matcher(content);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String matched = content.substring(start, end);
            if (matched.contains("\"")) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasWindowsWalletDirectory(String content) {
        if (content == null) {
            return false;
        }
        Matcher matcher = DIRECTORY.matcher(content);
        while (matcher.find()) {
            if (isWindowsPath(stripQuotes(matcher.group(2)))) {
                return true;
            }
        }
        return false;
    }

    public static void rewriteIfNeeded(Path walletDir) throws IOException {
        if (walletDir == null || !Files.isDirectory(walletDir)) {
            return;
        }
        String target = normalizeWalletDir(walletDir.toAbsolutePath().normalize().toString());
        for (String name : CONFIG_FILES) {
            Path file = walletDir.resolve(name);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            String original = Files.readString(file, StandardCharsets.UTF_8);
            String rewritten = rewriteContent(original, target);
            if (!original.equals(rewritten)) {
                Files.writeString(file, rewritten, StandardCharsets.UTF_8);
                log.info(
                        "wallet config rewritten file={} windows-directory={} directory-quoted={}",
                        name,
                        hasWindowsWalletDirectory(original),
                        hasQuotedDirectory(original));
            }
        }
    }

    public static boolean jdbcTnsAdminMatches(String jdbcUrl, String walletDir) {
        String urlDir = tnsAdminFromUrl(jdbcUrl);
        if (urlDir == null || walletDir == null) {
            return false;
        }
        return normalizeWalletDir(urlDir).equals(normalizeWalletDir(walletDir));
    }

    public static String tnsAdminFromUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        String marker = "tns_admin=";
        int index = jdbcUrl.toLowerCase(Locale.ROOT).indexOf(marker);
        if (index < 0) {
            return null;
        }
        String rest = jdbcUrl.substring(index + marker.length());
        int end = rest.indexOf('&');
        String value = end < 0 ? rest : rest.substring(0, end);
        value = normalizeWalletDir(value);
        return value == null || value.isBlank() ? null : value;
    }

    static String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.replace("\"", "");
    }

    private static boolean isWindowsPath(String path) {
        return path != null && path.length() >= 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':';
    }
}

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
 * 인증서·키 파일은 건드리지 않고, 그 경로만 현재 TNS_ADMIN으로 맞춘다.
 */
public final class WalletLocationRewriter {

    private static final Logger log = LoggerFactory.getLogger(WalletLocationRewriter.class);

    static final Pattern WINDOWS_DIRECTORY = Pattern.compile(
            "(DIRECTORY\\s*=\\s*)\"?([A-Za-z]:[\\\\/][^)\"]*)\"?",
            Pattern.CASE_INSENSITIVE);

    private static final String[] CONFIG_FILES = {"sqlnet.ora", "ojdbc.properties"};

    private WalletLocationRewriter() {
    }

    public static String rewriteContent(String content, String walletDir) {
        if (content == null || walletDir == null || walletDir.isBlank()) {
            return content;
        }
        String normalized = walletDir.replace('\\', '/');
        Matcher matcher = WINDOWS_DIRECTORY.matcher(content);
        StringBuilder updated = new StringBuilder();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            matcher.appendReplacement(
                    updated,
                    Matcher.quoteReplacement(matcher.group(1) + "\"" + normalized + "\""));
        }
        if (!found) {
            return content;
        }
        matcher.appendTail(updated);
        return updated.toString();
    }

    public static boolean hasWindowsWalletDirectory(String content) {
        return content != null && WINDOWS_DIRECTORY.matcher(content).find();
    }

    public static void rewriteIfNeeded(Path walletDir) throws IOException {
        if (walletDir == null || !Files.isDirectory(walletDir)) {
            return;
        }
        String target = walletDir.toAbsolutePath().normalize().toString().replace('\\', '/');
        for (String name : CONFIG_FILES) {
            Path file = walletDir.resolve(name);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            String original = Files.readString(file, StandardCharsets.UTF_8);
            if (!hasWindowsWalletDirectory(original)) {
                continue;
            }
            String rewritten = rewriteContent(original, target);
            if (!original.equals(rewritten)) {
                Files.writeString(file, rewritten, StandardCharsets.UTF_8);
                log.info("wallet config rewritten file={} windows-directory=yes", name);
            }
        }
    }

    public static boolean jdbcTnsAdminMatches(String jdbcUrl, String walletDir) {
        if (jdbcUrl == null || walletDir == null) {
            return false;
        }
        String marker = "tns_admin=";
        int index = jdbcUrl.toLowerCase(Locale.ROOT).indexOf(marker);
        if (index < 0) {
            return false;
        }
        String rest = jdbcUrl.substring(index + marker.length());
        int end = rest.indexOf('&');
        String urlDir = (end < 0 ? rest : rest.substring(0, end)).replace('\\', '/');
        String expected = walletDir.replace('\\', '/');
        return urlDir.equals(expected);
    }
}

package com.yunki.lessonpt.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WalletLocationRewriterTest {

    private static final String WINDOWS_SQLNET = """
            WALLET_LOCATION = (SOURCE = (METHOD = FILE) (METHOD_DATA = (DIRECTORY = "C:/Users/anian/OneDrive/Desktop/OCI-Wallet")))
            SSL_SERVER_DN_MATCH = yes
            """;

    @Test
    void rewritesQuotedWindowsDirectoryWithoutQuotes() {
        String rewritten = WalletLocationRewriter.rewriteContent(WINDOWS_SQLNET, "/var/lessonpt/wallet");
        assertThat(rewritten).contains("DIRECTORY = /var/lessonpt/wallet");
        assertThat(rewritten).doesNotContain("DIRECTORY = \"/var/lessonpt/wallet\"");
        assertThat(rewritten).doesNotContain("C:/Users");
        assertThat(rewritten).contains("SSL_SERVER_DN_MATCH = yes");
    }

    @Test
    void stripsQuotesFromAlreadyLinuxDirectory() {
        String original = "WALLET_LOCATION = (SOURCE = (METHOD = FILE) (METHOD_DATA = (DIRECTORY = \"/var/lessonpt/wallet\")))";
        String rewritten = WalletLocationRewriter.rewriteContent(original, "/var/lessonpt/wallet");
        assertThat(rewritten).contains("DIRECTORY = /var/lessonpt/wallet)");
        assertThat(rewritten).doesNotContain("DIRECTORY = \"/var/lessonpt/wallet\"");
        assertThat(WalletLocationRewriter.hasQuotedDirectory(original)).isTrue();
        assertThat(WalletLocationRewriter.hasQuotedDirectory(rewritten)).isFalse();
    }

    @Test
    void rewritesBackslashWindowsDirectory() {
        String original = "DIRECTORY = C:\\Users\\anian\\OneDrive\\Desktop\\OCI-Wallet)";
        String rewritten = WalletLocationRewriter.rewriteContent(original, "/var/lessonpt/wallet");
        assertThat(rewritten).contains("DIRECTORY = /var/lessonpt/wallet)");
        assertThat(rewritten).doesNotContain("OneDrive");
    }

    @Test
    void leavesUnquotedLinuxDirectoryUnchanged() {
        String original = "DIRECTORY = /var/lessonpt/wallet)";
        assertThat(WalletLocationRewriter.rewriteContent(original, "/var/lessonpt/wallet")).isEqualTo(original);
    }

    @Test
    void rewritesOjdbcPropertiesDirectory() {
        String original = "oracle.net.wallet_location=(SOURCE=(METHOD=FILE)(METHOD_DATA=(DIRECTORY=\"/var/lessonpt/wallet\")))";
        String rewritten = WalletLocationRewriter.rewriteContent(original, "/var/lessonpt/wallet");
        assertThat(rewritten).contains("DIRECTORY=/var/lessonpt/wallet)");
        assertThat(rewritten).doesNotContain("DIRECTORY=\"/var/lessonpt/wallet\"");
    }

    @Test
    void rewritesQuotedLinuxSqlnetFileAndLeavesWalletBinary(@TempDir Path walletDir) throws Exception {
        Path sqlnet = walletDir.resolve("sqlnet.ora");
        Path sso = walletDir.resolve("cwallet.sso");
        Files.writeString(sqlnet, "DIRECTORY = \"/var/lessonpt/wallet\")\n");
        byte[] binary = {0x01, 0x02, 0x03, 0x04};
        Files.write(sso, binary);

        WalletLocationRewriter.rewriteIfNeeded(walletDir);

        String expected = walletDir.toAbsolutePath().normalize().toString().replace('\\', '/');
        String sqlnetText = Files.readString(sqlnet, StandardCharsets.UTF_8);
        assertThat(sqlnetText).contains("DIRECTORY = " + expected);
        assertThat(sqlnetText).doesNotContain("DIRECTORY = \"" + expected + "\"");
        assertThat(Files.readAllBytes(sso)).containsExactly(binary);
    }

    @Test
    void jdbcTnsAdminMustMatchWalletDirAndIgnoreQuotes() {
        assertThat(WalletLocationRewriter.jdbcTnsAdminMatches(
                "jdbc:oracle:thin:@dbname_high?TNS_ADMIN=/var/lessonpt/wallet",
                "/var/lessonpt/wallet")).isTrue();
        assertThat(WalletLocationRewriter.jdbcTnsAdminMatches(
                "jdbc:oracle:thin:@dbname_high?TNS_ADMIN=\"/var/lessonpt/wallet\"",
                "/var/lessonpt/wallet")).isTrue();
        assertThat(WalletLocationRewriter.jdbcTnsAdminMatches(
                "jdbc:oracle:thin:@dbname_high?TNS_ADMIN=/var/lessonpt/wallet",
                "/var/lessonpt/wallet/nested")).isFalse();
    }

    @Test
    void shellScriptStripsQuotesFromLinuxDirectory(@TempDir Path walletDir) throws Exception {
        Path script = Path.of("scripts/rewrite-wallet-location.sh");
        Assumptions.assumeTrue(Files.isRegularFile(script), "rewrite script is present");
        String shell = resolveShell();
        Assumptions.assumeTrue(shell != null, "POSIX shell is available");
        Files.writeString(
                walletDir.resolve("sqlnet.ora"),
                "WALLET_LOCATION = (SOURCE = (METHOD = FILE) (METHOD_DATA = (DIRECTORY = \"/var/lessonpt/wallet\")))\n");
        Files.write(walletDir.resolve("cwallet.sso"), new byte[] {9, 8, 7});

        Process process = new ProcessBuilder(shell, script.toAbsolutePath().toString(), walletDir.toString())
                .redirectErrorStream(true)
                .start();
        Assumptions.assumeTrue(process.waitFor(20, TimeUnit.SECONDS), "shell rewrite finished");
        Assumptions.assumeTrue(process.exitValue() == 0, "shell rewrite succeeded");

        String sqlnet = Files.readString(walletDir.resolve("sqlnet.ora"));
        String expected = walletDir.toAbsolutePath().normalize().toString().replace('\\', '/');
        assertThat(sqlnet).contains("DIRECTORY = " + expected);
        assertThat(sqlnet).doesNotContain("DIRECTORY = \"" + expected + "\"");
        assertThat(sqlnet).doesNotContain("DIRECTORY = \"/var/lessonpt/wallet\"");
        assertThat(Files.readAllBytes(walletDir.resolve("cwallet.sso"))).containsExactly(9, 8, 7);
    }

    private static String resolveShell() {
        for (String candidate : new String[] {"sh", "bash", "C:\\Program Files\\Git\\bin\\sh.exe"}) {
            try {
                Process probe = new ProcessBuilder(candidate, "-c", "exit 0").start();
                if (probe.waitFor(5, TimeUnit.SECONDS) && probe.exitValue() == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // try the next shell
            }
        }
        return null;
    }
}

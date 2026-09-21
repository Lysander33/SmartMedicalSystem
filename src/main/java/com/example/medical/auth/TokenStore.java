package com.example.medical.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录令牌存储。
 * 令牌会落盘（系统临时目录），所以重启 Spring Boot 不会把所有人踢下线；
 * 课设项目够用；要支持多实例，换成 Redis / JWT 即可，接口不变。
 */
@Slf4j
@Component
public class TokenStore {

    /** 有效期 2 小时，每次访问滑动续期 */
    private static final long TTL_MILLIS = 2 * 60 * 60 * 1000L;

    /** 落盘位置：系统临时目录，不进项目目录，避免被 mvn clean 清掉 */
    private static final Path STORE_FILE =
            Path.of(System.getProperty("java.io.tmpdir"), "smart-medical-tokens.tsv");

    private record Entry(LoginUser user, long expireAt) {
    }

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    public TokenStore() {
        load();
    }

    /** 登录成功签发令牌 */
    public String issue(LoginUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new Entry(user, System.currentTimeMillis() + TTL_MILLIS));
        save();
        return token;
    }

    /** 解析令牌，无效或过期返回 null */
    public LoginUser get(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Entry entry = tokens.get(token);
        if (entry == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (entry.expireAt() < now) {
            tokens.remove(token);
            save();
            return null;
        }
        long remaining = entry.expireAt() - now;
        tokens.put(token, new Entry(entry.user(), now + TTL_MILLIS));
        //滑动续期；磁盘上的过期时间每消耗过半才回写一次，避免每个请求都写文件
        if (remaining < TTL_MILLIS / 2) {
            save();
        }
        return entry.user();
    }

    public void remove(String token) {
        if (token != null && tokens.remove(token) != null) {
            save();
        }
    }

    // ==================== 落盘 ====================

    /** 一行一个令牌：token \t userId \t username \t role \t refId \t name \t expireAt */
    private void load() {
        if (!Files.exists(STORE_FILE)) {
            return;
        }
        long now = System.currentTimeMillis();
        try {
            for (String line : Files.readAllLines(STORE_FILE, StandardCharsets.UTF_8)) {
                String[] f = line.split("\t", -1);
                if (f.length < 7) {
                    continue;
                }
                try {
                    long expireAt = Long.parseLong(f[6].trim());
                    if (expireAt < now) {
                        continue;
                    }
                    LoginUser user = new LoginUser(parseId(f[1]), f[2], f[3], parseId(f[4]), f[5]);
                    tokens.put(f[0], new Entry(user, expireAt));
                } catch (NumberFormatException bad) {
                    log.warn("跳过一条无法解析的令牌记录: {}", line);
                }
            }
            log.info("恢复登录令牌 {} 条（来源 {}）", tokens.size(), STORE_FILE);
        } catch (IOException e) {
            log.warn("读取令牌文件失败，按全部未登录处理: {}", e.getMessage());
        }
    }

    private synchronized void save() {
        List<String> lines = new ArrayList<>();
        long now = System.currentTimeMillis();
        tokens.forEach((token, entry) -> {
            if (entry.expireAt() < now) {
                return;
            }
            LoginUser u = entry.user();
            lines.add(String.join("\t",
                    token,
                    idText(u.getUserId()),
                    clean(u.getUsername()),
                    clean(u.getRole()),
                    idText(u.getRefId()),
                    clean(u.getName()),
                    String.valueOf(entry.expireAt())));
        });
        try {
            Path tmp = STORE_FILE.resolveSibling(STORE_FILE.getFileName() + ".tmp");
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            Files.move(tmp, STORE_FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("写入令牌文件失败，本次仍按内存中的登录态处理: {}", e.getMessage());
        }
    }

    /** 制表符/换行会破坏行格式（姓名里出现概率极低，兜一下底） */
    private String clean(String s) {
        return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    /** refId 对药房/管理员为空，写成空串而不是 "null"，否则回读时会当成数字解析失败 */
    private String idText(Integer id) {
        return id == null ? "" : String.valueOf(id);
    }

    private Integer parseId(String s) {
        return (s == null || s.isBlank() || "null".equals(s)) ? null : Integer.valueOf(s.trim());
    }
}

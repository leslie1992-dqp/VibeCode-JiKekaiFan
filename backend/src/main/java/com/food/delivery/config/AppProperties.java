package com.food.delivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自定义配置前缀 {@code app}，供 IDE 与 spring-configuration-metadata 识别。
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public static class Upload {
        /** 相对运行目录或绝对路径 */
        private String dir = "food-delivery-uploads";

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }
    }

    public static class Jwt {
        /**
         * HS256 签名密钥，生产环境务必替换为足够长度随机串。
         */
        private String secret = "";
        private int expireHours = 24;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getExpireHours() {
            return expireHours;
        }

        public void setExpireHours(int expireHours) {
            this.expireHours = expireHours;
        }
    }
}

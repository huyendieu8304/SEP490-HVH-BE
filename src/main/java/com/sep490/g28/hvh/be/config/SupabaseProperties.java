package com.sep490.g28.hvh.be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds Supabase-related configuration properties.
 *
 * <p>Maps properties with prefix {@code supabase}
 * to a configuration object used across the application.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "supabase")
@Getter
@Setter
public class SupabaseProperties {
    private String url;
    private String apiSecretKey; // aka service role key/ service key...
    private String bucket;
    private int uploadUrlExpireTimInSeconds= 600; //seconds = 10 minutes
    private int viewUrlExpireTimeInSeconds = 900; //15 minutes
}

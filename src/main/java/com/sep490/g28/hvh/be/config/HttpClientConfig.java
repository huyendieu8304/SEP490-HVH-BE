package com.sep490.g28.hvh.be.config;

import com.sep490.g28.hvh.be.exception.SupabaseResponseErrorHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client configuration for external service communication.
 */
@Configuration
public class HttpClientConfig {

    /**
     * RestTemplate used to send JSON requests to Supabase services.
     *
     * @param config Supabase configuration properties
     * @return configured {@link RestTemplate} bean
     */
    @Bean
    @Qualifier("supabaseRestTemplate")
    public RestTemplate supabaseRestTemplate(SupabaseProperties config) {
        RestTemplate rt = new RestTemplate();

        rt.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().setBearerAuth(config.getApiSecretKey());
            request.getHeaders().set("apikey", config.getApiSecretKey());

            // ONLY set Content-Type when body exist
            if (body.length > 0) {
                request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            }
            return execution.execute(request, body);
        });

        rt.setErrorHandler(new SupabaseResponseErrorHandler());
        return rt;
    }

    /**
     * RestTemplate used to send multipart/form-data requests to Face API services.
     *
     * @return configured {@link RestTemplate} bean
     */
    @Bean
    @Qualifier("faceapiRestTemplate")
    public RestTemplate faceapiRestTemplate() {
        RestTemplate rt = new RestTemplate();

        rt.getInterceptors().add((request, body, execution) -> {

            return execution.execute(request, body);
        });

        rt.setErrorHandler(new SupabaseResponseErrorHandler());
        return rt;
    }


}

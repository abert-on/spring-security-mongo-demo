package security;

import com.mongodb.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfiguration {

    public static final String DB_HOST = "vb-ubuntu";
    public static final int DB_PORT = 27017;

    @Bean
    public MongoClient createConnection() {
        return new MongoClient(DB_HOST, DB_PORT);
    }
}

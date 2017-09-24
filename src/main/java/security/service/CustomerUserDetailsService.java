package security.service;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import gherkin.deps.com.google.gson.Gson;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import security.domain.User;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@Service
public class CustomerUserDetailsService implements UserDetailsService, IUserCreationService {

    private static final String DATABASE_NAME = "securitydemo1";

    @Autowired
    private MongoClient mongoClient;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        final MongoCollection<Document> collection = getUsersCollection();

        final Document document = collection.find(eq("username", username)).first();

        if (document != null) {
            final ObjectId id = document.get("_id", ObjectId.class);
            final String firstName = document.getString("firstName");
            final String lastName = document.getString("lastName");
            final String password = document.getString("password");
            final List<String> authorities = (List<String>) document.get("grantedAuthorities");
            final Boolean enabled = document.getBoolean("enabled");

            final User user = new User();
            user.setId(id.toString());
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUsername(username);
            user.setPassword(password);
            user.setGrantedAuthorities(authorities);
            user.setEnabled(enabled);

            return user;
        }

        throw new UsernameNotFoundException("User " + username + " not found.");
    }

    @Override
    public void save(final User user) {
        final MongoCollection<Document> collection = getUsersCollection();

        if (collection.find(eq("username", user.getUsername())).first() == null) {
            collection.insertOne(Document.parse(new Gson().toJson(user)));
        }
        else {
            collection.updateOne(eq("username", user.getUsername()), new Document("$set", Document.parse(new Gson().toJson(user))));
        }
    }

    @Override
    public boolean emailExists(final String email) {
        final MongoCollection<Document> collection = getUsersCollection();

        return null != collection.find(eq("username", email)).first();
    }

    @Override
    public User findByConfirmationToken(final String token) {
        final MongoCollection<Document> collection = getUsersCollection();

        return new Gson().fromJson(collection.find(eq("confirmationToken", token)).first().toJson(), User.class);
    }

    private MongoCollection<Document> getUsersCollection() {
        final MongoDatabase db = this.mongoClient.getDatabase(DATABASE_NAME);
        return db.getCollection("users");
    }
}

package tw.com.ehanlin.tomcatMongodbSession;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.session.StandardSession;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.bson.Document;

import java.io.IOException;

public class MongoStandardManager extends StandardManager {

    private static final Log log = LogFactory.getLog(MongoStandardManager.class);

    private String uri;
    public void setUri(String uri) {
        this.uri = uri;
    }
    public String getUri() {
        return this.uri;
    }

    private String db;
    public void setDb(String db) {
        this.db = db;
    }
    public String getDb() {
        return this.db;
    }

    private String collection;
    public void setCollection(String collection) {
        this.collection = collection;
    }
    public String getCollection() {
        return this.collection;
    }

    private MongoClient client;
    private MongoCollection<Document> coll;

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        log.info("initInternal uri=" + uri + " db=" + db + " collection=" + collection);
        this.client = MongoClients.create(uri);
        this.coll = client.getDatabase(db).getCollection(collection);
    }

    @Override
    public Session findSession(String id) throws IOException {
        return this.createSession(id);
    }

    @Override
    protected StandardSession getNewSession() {
        return new MongoSession(this, this.coll);
    }
}

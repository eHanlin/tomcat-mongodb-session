package tw.com.ehanlin.tomcatMongodbSession;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.*;

import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import org.apache.catalina.Session;
import org.apache.catalina.session.PersistentManager;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.session.StoreBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.bson.Document;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

final public class MongoStore extends StoreBase {

    private static final Log log = LogFactory.getLog(MongoStore.class);

    private String uri;
    public void setUri(String uri) {
        this.uri = uri;
    }
    public String getUri() {
        return uri;
    }

    private String db;
    public void setDb(String db) {
        this.db = db;
    }
    public String getDb() {
        return db;
    }

    private String collection;
    public void setCollection(String collection) {
        this.collection = collection;
    }
    public String getCollection() {
        return collection;
    }

    private MongoClient client;
    private MongoCollection<Document> coll;

    @Override
    protected void initInternal() {
        super.initInternal();
        log.info("initInternal uri=" + uri + " db=" + db + " collection=" + collection);
        client = MongoClients.create(uri);
        coll = client.getDatabase(db).getCollection(collection);
    }

    @Override
    public int getSize() throws IOException {
        log.info("getSize");
        return (int) coll.countDocuments();
    }

    @Override
    public String[] keys() throws IOException {
        log.info("keys");
        ArrayList<String> list = new ArrayList<>();
        MongoCursor<String> iterator = coll.distinct("_id", String.class).iterator();
        iterator.forEachRemaining((key) -> list.add(key));
        String[] result = new String[list.size()];
        result = list.toArray(result);
        return result;
    }

    @Override
    public Session load(String id) throws ClassNotFoundException, IOException {
        log.info("load id=" + id);
        HttpSession session = (HttpSession) this.manager.createSession(id);
        try{
            Document sessionData = coll.find(eq("_id", id)).first();
            if(sessionData != null){
                sessionData.forEach((k, v) -> session.setAttribute(k, v));
            }
            log.info("loaded id=" + id);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return (Session) session;
    }

    @Override
    public void save(Session session) throws IOException {
        log.info("save id=" + session.getId());
        try {
            HttpSession httpSession = (HttpSession) session;
            Enumeration<String> names = httpSession.getAttributeNames();
            Document doc = new Document();
            while (names.hasMoreElements()) {
                String k = names.nextElement();
                doc.put(k, httpSession.getAttribute(k));
            }
            coll.replaceOne(eq("_id", session.getId()), doc, new ReplaceOptions().upsert(true));
            log.info("saved id=" + session.getId());
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void remove(String id) throws IOException {
        log.info("remove id=" + id);
        try {
            coll.deleteOne(eq("_id", id));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void clear() throws IOException {
        log.info("clear");
        try {
            coll.drop();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}

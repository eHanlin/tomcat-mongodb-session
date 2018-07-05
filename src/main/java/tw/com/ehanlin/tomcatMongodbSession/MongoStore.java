package tw.com.ehanlin.tomcatMongodbSession;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.*;

import com.mongodb.client.model.UpdateOptions;
import org.apache.catalina.Session;
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

    private MongoCollection<Document> coll;

    @Override
    protected void initInternal() {
        super.initInternal();
        log.info("initInternal uri=" + uri + " db=" + db + " collection=" + collection);
        this.coll = new MongoClient(new MongoClientURI(this.uri))
                .getDatabase(this.db)
                .getCollection(this.collection);
    }

    @Override
    public int getSize() throws IOException {
        log.info("getSize");
        return (int) coll.count();
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
        Document sessionData = coll.find(eq("_id", id)).first();
        if(sessionData != null){
            HttpSession session = (HttpSession) this.manager.createSession(id);
            sessionData.forEach((k, v) -> session.setAttribute(k, v));
            log.info("loaded id=" + id);
            return (Session) session;
        }else{
            return null;
        }
    }

    @Override
    public void save(Session session) throws IOException {
        save((HttpSession) session);
    }

    private static UpdateOptions upsertOptions = new UpdateOptions().upsert(true);

    public void save(HttpSession httpSession) {
        log.info("save id=" + httpSession.getId());
        Enumeration<String> names = httpSession.getAttributeNames();
        Document doc = new Document();
        while (names.hasMoreElements()) {
            String k = names.nextElement();
            doc.put(k, httpSession.getAttribute(k));
        }
        coll.replaceOne(eq("_id", httpSession.getId()), doc, upsertOptions);
        log.info("saved id=" + httpSession.getId());
    }

    @Override
    public void remove(String id) throws IOException {
        log.info("remove id=" + id);
        coll.deleteOne(eq("_id", id));
    }

    @Override
    @Deprecated
    public void clear() throws IOException {
        log.info("clear");
        //coll.drop();
    }
}

package tw.com.ehanlin.tomcatMongodbSession;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.*;
import org.apache.catalina.Session;
import org.apache.catalina.session.StoreBase;
import org.bson.Document;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

final public class Store extends StoreBase {

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
        client = MongoClients.create(uri);
        coll = client.getDatabase(db).getCollection(collection);
    }

    @Override
    public int getSize() throws IOException {
        return (int) coll.countDocuments();
    }

    @Override
    public String[] keys() throws IOException {
        ArrayList<String> list = new ArrayList<>();
        MongoCursor<String> iterator = coll.distinct("_id", String.class).iterator();
        iterator.forEachRemaining((key) -> list.add(key));
        return (String[]) list.toArray();
    }

    @Override
    public Session load(String id) throws ClassNotFoundException, IOException {
        HttpSession session = (HttpSession) this.manager.createSession(id);
        try{
            Document sessionData = coll.find(eq("_id", id)).first();
            if(sessionData != null){
                sessionData.forEach((k, v) -> session.setAttribute(k, v));
            }
        } catch (Throwable ex) {

        }
        return (Session) session;
    }

    @Override
    public void save(Session session) throws IOException {
        try {
            HttpSession httpSession = (HttpSession) session;
            Enumeration<String> names = httpSession.getAttributeNames();
            Document setDoc = new Document();
            while (names.hasMoreElements()) {
                String k = names.nextElement();
                setDoc.put(k, httpSession.getAttribute(k));
            }
            coll.updateOne(eq("_id", session.getId()), new Document("$set", setDoc));
        } catch (Throwable ex) {

        }
    }

    @Override
    public void remove(String id) throws IOException {
        try {
            coll.deleteOne(eq("_id", id));
        } catch (Throwable ex) {

        }
    }

    @Override
    public void clear() throws IOException {
        try {
            coll.drop();
        } catch (Throwable ex) {

        }
    }
}

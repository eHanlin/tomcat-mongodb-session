package tw.com.ehanlin.tomcatMongodbSession;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;

final public class MongoSession extends StandardSession {

    private static final Log log = LogFactory.getLog(MongoSession.class);

    private static final String SESSION_ID = "_id";
    private static final String CREATE_DATE = "_createDate";
    private static final String LAST_UPDATE_DATE = "_lastUpdateDate";

    private static final FindOneAndUpdateOptions findUpsertOptions = new FindOneAndUpdateOptions().upsert(true);
    private static final UpdateOptions upsertOptions = new UpdateOptions().upsert(true);

    private static final Pattern systemKeyPattern = Pattern.compile("^_.*");
    private static boolean isSystemKey(String key) {
        return systemKeyPattern.matcher(key).matches();
    }

    public MongoSession(Manager manager) {
        super(manager);
    }

    public MongoSession(Manager manager, MongoCollection<Document> coll) {
        super(manager);
        this.coll = coll;
    }

    private MongoCollection<Document> coll;
    public void setColl(MongoCollection<Document> coll) {
        this.coll = coll;
    }
    public MongoCollection<Document> getColl() {
        return coll;
    }

    @Override
    public void setId(String id, boolean notify) {
        //log.info("setAttribute id=" + id);
        super.setId(id, notify);
        syncFromMongo();
    }

    @Override
    public void setAttribute(String name, Object value, boolean notify) {
        //log.info("setAttribute id=" + this.id);
        super.setAttribute(name, value, notify);
        syncAppendOneAttribute(name);
    }

    @Override
    protected void removeAttributeInternal(String name, boolean notify) {
        //log.info("removeAttributeInternal id=" + this.id);
        super.removeAttributeInternal(name, notify);
        syncRemoveOneAttribute(name);
    }

    private void syncFromMongo() {
        //log.info("syncFromMongo id=" + this.id);
        long now = System.currentTimeMillis();
        Document sessionData = coll.findOneAndUpdate(
            eq(SESSION_ID, this.id),
            new Document("$setOnInsert", new Document(CREATE_DATE, now).append(LAST_UPDATE_DATE, now)),
            findUpsertOptions);

        if(sessionData != null){
            sessionData.forEach((k, v) -> {if(k != null && v != null && !isSystemKey(k)) this.attributes.put(k, v);});
        }
        //log.info("syncedFromMongo id=" + this.id);
    }

    private void syncToMongo() {
        //log.info("syncToMongo id=" + this.id);
        Document doc = new Document();
        this.attributes.forEach((k, v) -> doc.put(k, v));
        doc.put(LAST_UPDATE_DATE, System.currentTimeMillis());

        coll.replaceOne(eq(SESSION_ID, this.id), doc, upsertOptions);
        //log.info("syncedToMongo id=" + this.id);
    }

    private void syncAppendOneAttribute(String name) {
        //log.info("syncAppendOneAttribute id=" + this.id + "name=" + name);
        coll.updateOne(
            eq(SESSION_ID, this.id),
            new Document("$set", new Document(name, this.attributes.get(name)).append(LAST_UPDATE_DATE, System.currentTimeMillis())),
            upsertOptions);
    }

    private void syncRemoveOneAttribute(String name) {
        //log.info("syncRemoveOneAttribute id=" + this.id + "name=" + name);
        if(!isSystemKey(name)){
            coll.updateOne(
                eq(SESSION_ID, this.id),
                new Document("$set", new Document(LAST_UPDATE_DATE, System.currentTimeMillis()))
                    .append("$unset", new Document(name, "")));
        }
    }
}

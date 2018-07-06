package tw.com.ehanlin.tomcatMongodbSession;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;

final public class MongoSession extends StandardSession {

    private static final Log log = LogFactory.getLog(MongoSession.class);

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
        Document sessionData = coll.find(eq("_id", this.id)).first();
        if(sessionData != null){
            sessionData.forEach((k, v) -> this.attributes.put(k, v));
        }
        //log.info("syncedFromMongo id=" + this.id);
    }

    private static UpdateOptions upsertOptions = new UpdateOptions().upsert(true);

    private void syncToMongo() {
        //log.info("syncToMongo id=" + this.id);
        Document doc = new Document();
        this.attributes.forEach((k, v) -> doc.put(k, v));
        coll.replaceOne(eq("_id", this.id), doc, upsertOptions);
        //log.info("syncedToMongo id=" + this.id);
    }

    private void syncAppendOneAttribute(String name) {
        //log.info("syncAppendOneAttribute id=" + this.id + "name=" + name);
        coll.updateOne(eq("_id", this.id), set(name, this.attributes.get(name)), upsertOptions);
    }

    private void syncRemoveOneAttribute(String name) {
        //log.info("syncRemoveOneAttribute id=" + this.id + "name=" + name);
        coll.updateOne(eq("_id", this.id), unset(name));
    }
}

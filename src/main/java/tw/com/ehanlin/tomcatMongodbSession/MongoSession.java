package tw.com.ehanlin.tomcatMongodbSession;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.UpdateOptions;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.TomcatPrincipal;
import org.apache.tomcat.util.ExceptionUtils;
import org.bson.Document;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

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
        this.removeAttributeInternal(name, notify, true);
    }

    protected void removeAttributeInternal(String name, boolean notify, boolean sync) {
        //log.info("removeAttributeInternal id=" + this.id);
        super.removeAttributeInternal(name, notify);
        if(sync){
            syncRemoveOneAttribute(name);
        }
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

    @Override
    public void expire(boolean notify) {
        //log.info("expire id=" + this.id);
        if (this.isValid) {
            synchronized(this) {
                if (!this.expiring && this.isValid) {
                    if (this.manager != null) {
                        this.expiring = true;
                        Context context = this.manager.getContext();
                        if (notify) {
                            ClassLoader oldContextClassLoader = null;

                            try {
                                oldContextClassLoader = context.bind(Globals.IS_SECURITY_ENABLED, (ClassLoader)null);
                                Object[] listeners = context.getApplicationLifecycleListeners();
                                if (listeners != null && listeners.length > 0) {
                                    HttpSessionEvent event = new HttpSessionEvent(this.getSession());

                                    for(int i = 0; i < listeners.length; ++i) {
                                        int j = listeners.length - 1 - i;
                                        if (listeners[j] instanceof HttpSessionListener) {
                                            HttpSessionListener listener = (HttpSessionListener)listeners[j];

                                            try {
                                                context.fireContainerEvent("beforeSessionDestroyed", listener);
                                                listener.sessionDestroyed(event);
                                                context.fireContainerEvent("afterSessionDestroyed", listener);
                                            } catch (Throwable var29) {
                                                ExceptionUtils.handleThrowable(var29);

                                                try {
                                                    context.fireContainerEvent("afterSessionDestroyed", listener);
                                                } catch (Exception var28) {
                                                    ;
                                                }

                                                this.manager.getContext().getLogger().error(sm.getString("standardSession.sessionEvent"), var29);
                                            }
                                        }
                                    }
                                }
                            } finally {
                                context.unbind(Globals.IS_SECURITY_ENABLED, oldContextClassLoader);
                            }
                        }

                        if (ACTIVITY_CHECK) {
                            this.accessCount.set(0);
                        }

                        this.manager.remove(this, true);
                        if (notify) {
                            this.fireSessionEvent("destroySession", (Object)null);
                        }

                        if (this.principal instanceof TomcatPrincipal) {
                            TomcatPrincipal gp = (TomcatPrincipal)this.principal;

                            try {
                                gp.logout();
                            } catch (Exception var27) {
                                this.manager.getContext().getLogger().error(sm.getString("standardSession.logoutfail"), var27);
                            }
                        }

                        this.setValid(false);
                        this.expiring = false;
                        String[] keys = this.keys();
                        ClassLoader oldContextClassLoader = null;

                        try {
                            oldContextClassLoader = context.bind(Globals.IS_SECURITY_ENABLED, (ClassLoader)null);

                            for(int i = 0; i < keys.length; ++i) {
                                this.removeAttributeInternal(keys[i], notify, false);
                            }
                        } finally {
                            context.unbind(Globals.IS_SECURITY_ENABLED, oldContextClassLoader);
                        }

                    }
                }
            }
        }
    }
}

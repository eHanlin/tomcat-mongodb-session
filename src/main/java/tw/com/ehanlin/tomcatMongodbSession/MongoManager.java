package tw.com.ehanlin.tomcatMongodbSession;

import org.apache.catalina.*;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final public class MongoManager extends LifecycleMBeanBase implements Manager {

    private static final Log log = LogFactory.getLog(MongoManager.class);

    private class MongoHttpSessionAttributeListener implements HttpSessionAttributeListener {

        private final Log log = LogFactory.getLog(MongoHttpSessionAttributeListener.class);

        private MongoStore store = null;
        public void setStore(MongoStore store) {
            this.store = store;
        }
        public MongoStore getStore() {
            return store;
        }

        @Override
        public void attributeAdded(HttpSessionBindingEvent httpSessionBindingEvent) {
            log.info("attributeAdded id=" + httpSessionBindingEvent.getSession().getId());
            this.store.save(httpSessionBindingEvent.getSession());

        }

        @Override
        public void attributeRemoved(HttpSessionBindingEvent httpSessionBindingEvent) {
            log.info("attributeRemoved id=" + httpSessionBindingEvent.getSession().getId());
            this.store.save(httpSessionBindingEvent.getSession());
        }

        @Override
        public void attributeReplaced(HttpSessionBindingEvent httpSessionBindingEvent) {
            log.info("attributeReplaced id=" + httpSessionBindingEvent.getSession().getId());
            this.store.save(httpSessionBindingEvent.getSession());
        }

    }

    private MongoHttpSessionAttributeListener mongoHttpSessionAttributeListener = new MongoHttpSessionAttributeListener();


    private MongoStore store = null;
    public void setStore(MongoStore store) {
        store.setManager(this);
        this.store = store;
        mongoHttpSessionAttributeListener.setStore(store);
    }
    public MongoStore getStore() {
        return store;
    }

    private MongoMap sessions = new MongoMap();

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        log.info("initInternal");
        if(this.store != null){
            log.info("initInternal");
            ((Lifecycle) this.store).start();
            sessions.setStore(this.store);
        }
    }

    @Override
    protected String getDomainInternal() {
        log.info("getDomainInternal");
        return this.context.getDomain();
    }

    @Override
    protected String getObjectNameKeyProperties() {
        log.info("getObjectNameKeyProperties");
        StringBuilder name = new StringBuilder("type=Manager");
        name.append(",host=");
        name.append(this.context.getParent().getName());
        name.append(",context=");
        String contextName = this.context.getName();
        if (!contextName.startsWith("/")) {
            name.append('/');
        }

        name.append(contextName);
        return name.toString();
    }


    private Context context;
    @Override
    public Context getContext() {
        log.info("getContext");
        return this.context;
    }
    @Override
    public void setContext(Context context) {
        log.info("setContext");
        if(this.context != null){
            List<Object> oldEventListeners = Arrays.asList(this.context.getApplicationEventListeners());
            oldEventListeners.remove(this.mongoHttpSessionAttributeListener);
            this.context.setApplicationEventListeners(oldEventListeners.toArray());
        }

        this.context = context;

        if(this.context != null){
            if(this.context.getApplicationEventListeners() != null){
                List<Object> eventListeners = new ArrayList<>(Arrays.asList(this.context.getApplicationEventListeners()));
                eventListeners.add(this.mongoHttpSessionAttributeListener);
                this.context.setApplicationEventListeners(eventListeners.toArray());
            }else{
                Object[] eventListenerArray = { this.mongoHttpSessionAttributeListener };
                this.context.setApplicationEventListeners(eventListenerArray);
            }
        }
    }

    private SessionIdGenerator sessionIdGenerator = null;
    private Class<? extends SessionIdGenerator> sessionIdGeneratorClass = null;
    @Override
    public SessionIdGenerator getSessionIdGenerator() {
        log.info("getSessionIdGenerator");
        if (this.sessionIdGenerator != null) {
            return this.sessionIdGenerator;
        } else {
            if (this.sessionIdGeneratorClass != null) {
                try {
                    this.sessionIdGenerator = (SessionIdGenerator)this.sessionIdGeneratorClass.getConstructor().newInstance();
                    return this.sessionIdGenerator;
                } catch (ReflectiveOperationException var2) {

                }
            }

            return null;
        }
    }

    @Override
    public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
        log.info("setSessionIdGenerator");
        this.sessionIdGenerator = sessionIdGenerator;
        this.sessionIdGeneratorClass = sessionIdGenerator.getClass();
    }


    @Override
    public long getSessionCounter() {
        log.info("getSessionCounter");
        return getMaxActive();
    }

    @Override
    @Deprecated
    public void setSessionCounter(long l) {
        log.info("!!!setSessionCounter");
    }

    private volatile int maxActive = 0;
    @Override
    public int getMaxActive() {
        log.info("getMaxActive");
        return this.maxActive;
    }

    @Override
    public void setMaxActive(int i) {
        log.info("setMaxActive");
        this.maxActive = i;
    }

    @Override
    public int getActiveSessions() {
        log.info("getActiveSessions");
        try {
            return store.getSize();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    @Deprecated
    public long getExpiredSessions() {
        log.info("!!!getExpiredSessions");
        return 0;
    }

    @Override
    @Deprecated
    public void setExpiredSessions(long l) {
        log.info("!!!setExpiredSessions");
    }

    @Override
    @Deprecated
    public int getRejectedSessions() {
        log.info("!!!getRejectedSessions");
        return 0;
    }

    private volatile int sessionMaxAliveTime;
    @Override
    public int getSessionMaxAliveTime() {
        log.info("getSessionMaxAliveTime");
        return this.sessionMaxAliveTime;
    }

    @Override
    public void setSessionMaxAliveTime(int i) {
        log.info("setSessionMaxAliveTime");
        this.sessionMaxAliveTime = i;
    }

    @Override
    @Deprecated
    public int getSessionAverageAliveTime() {
        log.info("!!!getSessionAverageAliveTime");
        return 0;
    }

    @Override
    @Deprecated
    public int getSessionCreateRate() {
        log.info("!!!getSessionCreateRate");
        return 0;
    }

    @Override
    @Deprecated
    public int getSessionExpireRate() {
        log.info("!!!getSessionExpireRate");
        return 0;
    }

    @Override
    public void add(Session session) {
        log.info("add");
        sessions.put(session.getIdInternal(), session);
    }

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        log.info("addPropertyChangeListener");
        this.support.addPropertyChangeListener(propertyChangeListener);
    }
    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        log.info("removePropertyChangeListener");
        this.support.removePropertyChangeListener(propertyChangeListener);
    }

    private String generateSessionId() {
        log.info("generateSessionId");
        String result = null;
        do {
            result = this.sessionIdGenerator.generateSessionId();
        } while(this.sessions.containsKey(result));
        return result;
    }

    @Override
    public void changeSessionId(Session session) {
        log.info("changeSessionId session");
        String newId = this.generateSessionId();
        this.changeSessionId(session, newId, true, true);
    }

    @Override
    public void changeSessionId(Session session, String newId) {
        log.info("changeSessionId session newId");
        this.changeSessionId(session, newId, true, true);
    }

    private void changeSessionId(Session session, String newId, boolean notifySessionListeners, boolean notifyContainerListeners) {
        log.info("changeSessionId session newId notifySessionListeners notifyContainerListeners");
        String oldId = session.getIdInternal();
        session.setId(newId, false);
        session.tellChangedSessionId(newId, oldId, notifySessionListeners, notifyContainerListeners);
    }

    @Override
    public Session createEmptySession() {
        log.info("createEmptySession");
        return new StandardSession(this);
    }

    @Override
    public Session createSession(String sessionId) {
        log.info("createSession");
        Session session = this.createEmptySession();
        session.setNew(true);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        session.setMaxInactiveInterval(this.getContext().getSessionTimeout() * 60);
        String id = sessionId;
        if (sessionId == null) {
            id = this.generateSessionId();
        }
        session.setId(id, false);
        log.info("createdSession");
        return  session;
    }

    @Override
    public Session findSession(String id) throws IOException {
        log.info("findSession id");
        return id == null ? null : (Session)this.sessions.get(id);
    }

    @Deprecated
    @Override
    public Session[] findSessions() {
        log.info("!!!findSessions");
        return (Session[])this.sessions.values().toArray(new Session[0]);
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log.info("startInternal");
        SessionIdGenerator sessionIdGenerator = this.getSessionIdGenerator();
        if(sessionIdGenerator == null) {
            sessionIdGenerator = new StandardSessionIdGenerator();
            this.setSessionIdGenerator((SessionIdGenerator)sessionIdGenerator);
        }
        if(sessionIdGenerator instanceof Lifecycle) {
            ((Lifecycle)sessionIdGenerator).start();
        }

        MongoStore store = this.getStore();
        if(store == null){
            store = new MongoStore();
            this.setStore(store);
        }
        if(store instanceof Lifecycle) {
            ((Lifecycle)store).start();
        }

        this.setState(LifecycleState.STARTING);
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log.info("stopInternal");
        if(this.sessionIdGenerator instanceof Lifecycle) {
            ((Lifecycle)this.sessionIdGenerator).stop();
        }

        if(this.store instanceof Lifecycle) {
            ((Lifecycle)this.store).stop();
        }
    }

    @Override
    public void remove(Session session) {
        log.info("remove session");
        this.remove(session, false);
    }

    @Override
    public void remove(Session session, boolean update) {
        log.info("remove session update");
        if (session.getIdInternal() != null) {
            this.sessions.remove(session.getIdInternal());
        }
    }

    @Override
    @Deprecated
    public void backgroundProcess() {
        log.info("!!!backgroundProcess");
    }

    @Override
    @Deprecated
    public boolean willAttributeDistribute(String s, Object o) {
        log.info("!!!willAttributeDistribute");
        return false;
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
        log.info("load");
    }

    @Override
    public void unload() throws IOException {
        log.info("unload");
    }

}
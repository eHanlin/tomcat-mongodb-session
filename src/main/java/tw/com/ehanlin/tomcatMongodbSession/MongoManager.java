package tw.com.ehanlin.tomcatMongodbSession;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Store;
import org.apache.catalina.session.ManagerBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.IOException;

final public class MongoManager extends ManagerBase {

    private static final Log log = LogFactory.getLog(MongoManager.class);

    private Store store = null;
    public void setStore(Store store) {
        store.setManager(this);
        this.store = store;
    }
    public Store getStore() {
        return store;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        if(this.store != null){
            log.info("initInternal");
            ((Lifecycle) this.store).start();
            MongoMap mongoMap = new MongoMap();
            mongoMap.setStore(this.store);
            this.sessions = mongoMap;
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        super.startInternal();
        this.setState(LifecycleState.STARTING);
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {

    }

    @Override
    public void unload() throws IOException {

    }
}
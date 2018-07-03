package tw.com.ehanlin.tomcatMongodbSession;

import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.util.*;
import java.util.stream.Stream;

final public class MongoMap implements Map<String, Session> {

    private static final Log log = LogFactory.getLog(MongoMap.class);

    private Store store = null;
    public void setStore(Store store) {
        this.store = store;
    }
    public Store getStore() {
        return store;
    }

    @Override
    public int size() {
        try {
            return this.store.getSize();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            return Stream.of(this.store.keys()).anyMatch(k -> k.equals(key));
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    @Deprecated
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Session get(Object key) {
        try {
            return this.store.load((String) key);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Session put(String key, Session value) {
        try {
            this.store.save(value);
            return value;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Session remove(Object key) {
        try {
            Session result = this.get(key);
            this.store.remove((String) key);
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends Session> m) {
        m.forEach((k, v) -> this.put(k, v));
    }

    @Override
    @Deprecated
    public void clear() {

    }

    @Override
    public Set<String> keySet() {
        try {
            return new HashSet<>(Arrays.asList(this.store.keys()));
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    @Deprecated
    public Collection<Session> values() {
        return null;
    }

    @Override
    @Deprecated
    public Set<Entry<String, Session>> entrySet() {
        return null;
    }

}

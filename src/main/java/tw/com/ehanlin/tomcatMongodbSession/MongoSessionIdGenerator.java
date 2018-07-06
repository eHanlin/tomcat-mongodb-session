package tw.com.ehanlin.tomcatMongodbSession;

import com.mongodb.client.MongoCollection;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class MongoSessionIdGenerator extends StandardSessionIdGenerator {

    private static final Log log = LogFactory.getLog(MongoSessionIdGenerator.class);

    public MongoSessionIdGenerator(MongoCollection<Document> coll){
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
    public String generateSessionId(String route) {
        //log.info("generateSessionId");

        String id = null;

        do {
            id = super.generateSessionId(route);
            if(this.coll != null){
                try{
                    Document sessionData = coll.find(eq("_id", id)).first();
                    if(sessionData != null){
                        id = null;
                    }
                }catch(Throwable ex){

                }
            }
        } while (id == null);

        return id;
    }
}

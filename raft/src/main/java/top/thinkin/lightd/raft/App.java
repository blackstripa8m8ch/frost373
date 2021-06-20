package top.thinkin.lightd.raft;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import fi.iki.elonen.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.thinkin.lightd.db.DB;
import top.thinkin.lightd.db.ZSet;
import top.thinkin.lightd.exception.KitDBException;

import java.io.IOException;
import java.util.Map;

public class App extends NanoHTTPD {
    public App(int port) throws IOException {
        super(port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning! Point your browsers to http://localhost:8080/ \n");
    }

    private static final Logger LOG = LoggerFactory.getLogger(DBRequestProcessor.class);


    private KitRaft kitRaft;

    private static RouteTable rt;


    public static void main(String[] args) throws IOException, KitDBException {

        if (args.length != 6) {
            System.out
                    .println("Useage : java com.alipay.jraft.example.counter.CounterServer {dataPath} {groupId} {serverId} {initConf}");
            System.out
                    .println("Example: java com.alipay.jraft.example.counter.CounterServer /tmp/server1 counter 127.0.0.1:8081 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083");
            System.exit(1);
        }
        String dataPath = args[0];
        String groupId = args[1];
        String serverIdStr = args[2];
        String initConfStr = args[3];
        String dnName = args[4];
        String portStr = args[5];

        NodeOptions nodeOptions = new NodeOptions();
        // 为了测试, 调整 snapshot 间隔等参数
        nodeOptions.setElectionTimeoutMs(5000);
        nodeOptions.setDisableCli(false);
        nodeOptions.setSnapshotIntervalSecs(60 * 5);
        // 解析参数

        PeerId serverId = new PeerId();
        if (!serverId.parse(serverIdStr)) {
            throw new IllegalArgumentException("Fail to parse serverId:" + serverIdStr);
        }
        Configuration initConf = new Configuration();
        if (!initConf.parse(initConfStr)) {
            throw new IllegalArgumentException("Fail to parse initConf:" + initConfStr);
        }
        // 设置初始集群配置
        nodeOptions.setInitialConf(initConf);
        DB db = DB.build("D:\\temp\\" + dnName, false);


        KitRaft counterServer = new KitRaft(dataPath, groupId, serverId, nodeOptions, db, dnName);

        try {
            App app = new App(Integer.parseInt(portStr));
            app.kitRaft = counterServer;
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
        }

        rt = RouteTable.getInstance();



    }

    /*@Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";

        if ("/w/".equals(session.getUri())) {
            Map<String, String> parms = session.getParms();
            String k = parms.get("k");
            String v = parms.get("v");
            RKv rkv = this.db.getrKv();
            try {
                rkv.set(k, v.getBytes());
            } catch (KitDBException e) {
                e.printStackTrace();
            }
            return newFixedLengthResponse("</body></html>\n");
        } else if ("/r/".equals(session.getUri())) {
            RKv rkv = this.db.getrKv();
            Map<String, String> parms = session.getParms();
            String k = parms.get("k");
            ;
            try {
                return newFixedLengthResponse("</body>" + new String(rkv.get(k)) + "</html>\n");
            } catch (KitDBException e) {
                e.printStackTrace();
            }
        }

        return newFixedLengthResponse("</body></html>\n");

    }*/


    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";

        if ("/w/".equals(session.getUri())) {
            Map<String, String> parms = session.getParms();
            String m = parms.get("m");
            String s = parms.get("s");
            ZSet zset = this.kitRaft.getDB().getzSet();
            try {
                zset.add("text", m.getBytes(), Long.parseLong(s));

            } catch (KitDBException e) {
                e.printStackTrace();
            }
            return newFixedLengthResponse("</body></html>\n");
        } else if ("/r/".equals(session.getUri())) {
            ZSet zset = this.kitRaft.getDB().getzSet();
            Map<String, String> parms = session.getParms();
            String m = parms.get("m");
            try {

                return newFixedLengthResponse("</body>" + zset.score("text", m.getBytes()) + "</html>\n");
            } catch (KitDBException e) {
                e.printStackTrace();
            }
        } else if ("/getLeader".equals(session.getUri())) {
            try {
                return newFixedLengthResponse(kitRaft.getNode().getLeaderId().getEndpoint().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("/addPeer".equals(session.getUri())) {
            Map<String, String> parms = session.getParms();
            String node = parms.get("node");

            PeerId peer = new PeerId();
            peer.parse(node);
            kitRaft.getNode().addPeer(peer, s -> {
                LOG.error("addPeer error", s.getErrorMsg());
            });
        }
        return newFixedLengthResponse("</body></html>\n");

    }
}

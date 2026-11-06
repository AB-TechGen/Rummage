import dev.rummage.net.*;

/** Optional live check; creates a temporary room on the built-in Worker and closes it afterwards. */
class ProbeCloudRelay {
    public static void main(String[] args)throws Exception {
        try(Synchronizer server=new Synchronizer(0)){
            var bridge=CloudRelay.host(server.port(),s->{},new CloudRelay.Watcher(){public void reconnecting(){}public void back(){}public void lost(){}});
            try(LobbyConnection client=new LobbyConnection(bridge.code(),"Connection probe",s->{})){
                long deadline=System.nanoTime()+35_000_000_000L;
                while(client.state==null&&System.nanoTime()<deadline)Thread.sleep(50);
                if(client.state==null)throw new IllegalStateException("Live Worker join did not complete: "+client.status);
                System.out.println("PASS: built-in Cloudflare Worker hosted and joined a Rummage lobby.");
            }finally{bridge.close();}
        }
    }
}

package me.wowkfccc.orchestratorVelocity;

import org.yaml.snakeyaml.Yaml;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*;

public class VelocityConfig {
    // server
    public final String serverId;
    public final double capacityCpu, headroom;
    public final double capacityRamMb, headroomRam;

    // lstm
    public final boolean lstmEnabled; public final String lstmBase; public final int lstmTimeout;

    // regression
    public final double intercept, yLag1, yLag2; public final Map<String,Double> beta;

    // unit cost
    public final Map<String,Double> unitCostCpu, unitCostRamMb;

    // autoscale
    public final List<Integer> cpuSteps, ramStepsMb; public final int cooldownMin, upH, downH;

    // placement
    public final double migratePenalty, allowOver, scoreCpuW, scoreRamW;

    // mysql
    public final String jdbcUrl, jdbcUser, jdbcPass; public final int poolSize, horizon;
    public final String tblComp, tblLoad, tblPred;

    // misc
    public final boolean logPlanOnly;

    public VelocityConfig(Path dataDir) {
        try {
            if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
            Path cfg = dataDir.resolve("config.yml");
            if (!Files.exists(cfg)) writeDefault(cfg);
            Map<String,Object> root;
            try (InputStream in = Files.newInputStream(cfg)) { root = new Yaml().load(in); }
            if (root==null) root=new HashMap<>();

            Map<String,Object> server = m(root,"server");
            serverId = s(server,"id","proxy");
            capacityCpu = d(server,"capacity_cpu",100.0);
            headroom    = d(server,"headroom",0.80);
            capacityRamMb = d(server,"capacity_ram_mb",8192.0);
            headroomRam   = d(server,"headroom_ram",0.80);

            Map<String,Object> lstm = m(root,"lstm");
            lstmEnabled = b(lstm,"enabled",true);
            lstmBase    = s(lstm,"base_url","http://127.0.0.1:8900");
            lstmTimeout = i(lstm,"timeout_ms",1500);

            Map<String,Object> reg = m(root,"regression");
            intercept = d(reg,"intercept",5.0);
            Map<String,Object> ar = m(reg,"ar");
            yLag1 = d(ar,"y_lag1",0.20); yLag2=d(ar,"y_lag2",0.10);
            beta = readClassMap(m(reg,"beta"), 0.0);

            unitCostCpu   = readClassMap(m(root,"unit_cost_cpu"), 0.5);
            unitCostRamMb = readClassMap(m(root,"unit_cost_ram_mb"), 16.0);

            Map<String,Object> as = m(root,"autoscale");
            cpuSteps    = li(as,"cpu_steps",List.of(100,200,300));
            ramStepsMb  = li(as,"ram_steps_mb",List.of(5000,9000));
            cooldownMin = i(as,"cooldown_minutes",20);
            Map<String,Object> hy=m(as,"hysteresis"); upH=i(hy,"up",2); downH=i(hy,"down",3);

            Map<String,Object> pl = m(root,"placement");
            migratePenalty = d(pl,"migrate_penalty",0.5);
            allowOver      = d(pl,"allow_over_ratio",0.05);
            Map<String,Object> sc = m(pl,"score");
            scoreCpuW = d(sc,"cpu_weight",0.6);
            scoreRamW = d(sc,"ram_weight",0.4);

            Map<String,Object> mysql=m(root,"mysql");
            jdbcUrl=s(mysql,"url","jdbc:mysql://127.0.0.1:3306/mc_analytics?useSSL=false&serverTimezone=UTC");
            jdbcUser=s(mysql,"username","mc_user"); jdbcPass=s(mysql,"password","mc_pass");
            poolSize=i(mysql,"pool_size",8);

            Map<String,Object> tables=m(root,"tables");
            tblComp=s(tables,"comp","server_comp_30m");
            tblLoad=s(tables,"load","server_load_30m");
            tblPred=s(tables,"pred","player_type_pred");
            horizon=i(tables,"horizon",6);

            logPlanOnly = b(m(root,"velocity"),"log_plan_only",true);

        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public double effectiveCpu(){ return capacityCpu * headroom; }
    public double effectiveRamMb(){ return capacityRamMb * headroomRam; }

    // helpers
    @SuppressWarnings("unchecked") private static Map<String,Object> m(Map<String,Object> r,String k){ Object v=r.get(k); return v instanceof Map ? (Map<String,Object>)v : new HashMap<>();}
    private static String s(Map<String,Object> m,String k,String def){ Object v=m.get(k); return v==null?def:String.valueOf(v);}
    private static int i(Map<String,Object> m,String k,int def){ Object v=m.get(k); if(v instanceof Number n) return n.intValue(); try{return v==null?def:Integer.parseInt(v.toString());}catch(Exception e){return def;}}
    private static double d(Map<String,Object> m,String k,double def){ Object v=m.get(k); if(v instanceof Number n) return n.doubleValue(); try{return v==null?def:Double.parseDouble(v.toString());}catch(Exception e){return def;}}
    private static boolean b(Map<String,Object> m,String k,boolean def){ Object v=m.get(k); if(v instanceof Boolean bo) return bo; return v==null?def:Boolean.parseBoolean(v.toString()); }
    @SuppressWarnings("unchecked") private static List<Integer> li(Map<String,Object> m,String k,List<Integer> def){
        Object v=m.get(k); if(v instanceof List<?> l){ List<Integer> out=new ArrayList<>(); for(Object o:l){ if(o instanceof Number n) out.add(n.intValue()); else try{ out.add(Integer.parseInt(String.valueOf(o))); }catch(Exception ignore){} } return out.isEmpty()?def:out; } return def; }
    private static Map<String,Double> readClassMap(Map<String,Object> sec,double fb){
        Map<String,Double> m=new HashMap<>(); String[] ks={"AFK","Build","Explorer","Explosive","PvP","Redstone","Social","Survival"};
        for(String k:ks){ Object v=sec.get(k); double dv=(v instanceof Number n)? n.doubleValue(): (v==null?fb:parseD(v.toString(),fb)); m.put(k,dv); } return m;
    }
    private static double parseD(String s,double def){ try{ return Double.parseDouble(s);}catch(Exception e){ return def; } }

    private static void writeDefault(Path p) throws IOException {
        String def = """
    server:
      id: "proxy"
      capacity_cpu: 100.0
      headroom: 0.80
      capacity_ram_mb: 8192
      headroom_ram: 0.80
    lstm:
      enabled: true
      base_url: "http://127.0.0.1:8900"
      timeout_ms: 1500
    regression:
      intercept: 5.0
      beta: { AFK: 0.05, Build: 0.70, Explorer: 0.40, Explosive: 1.50, PvP: 1.60, Redstone: 1.40, Social: 0.35, Survival: 0.60 }
      ar: { y_lag1: 0.20, y_lag2: 0.10 }
    unit_cost_cpu: { AFK: 0.05, Build: 0.70, Explorer: 0.40, Explosive: 1.50, PvP: 1.60, Redstone: 1.40, Social: 0.35, Survival: 0.60 }
    unit_cost_ram_mb: { AFK: 5, Build: 30, Explorer: 20, Explosive: 25, PvP: 35, Redstone: 40, Social: 15, Survival: 28 }
    autoscale:
      cpu_steps: [100,200,300]
      ram_steps_mb: [5000,9000]
      cooldown_minutes: 20
      hysteresis: { up: 2, down: 3 }
    placement:
      migrate_penalty: 0.5
      allow_over_ratio: 0.05
      score: { cpu_weight: 0.6, ram_weight: 0.4 }
    mysql:
      url: "jdbc:mysql://127.0.0.1:3306/mc_analytics?useSSL=false&serverTimezone=UTC"
      username: "mc_user"
      password: "mc_pass"
      pool_size: 8
    tables:
      comp: "server_comp_30m"
      load: "server_load_30m"
      pred: "player_type_pred"
      horizon: 6
    velocity:
      log_plan_only: true
    """;
        Files.writeString(p, def, StandardCharsets.UTF_8);
    }
}

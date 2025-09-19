package me.wowkfccc.orchestratorVelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import me.wowkfccc.*;
import me.wowkfccc.Ports.*;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(id="mlp-orchestrator", name="MLP Orchestrator Velocity", version="1.0-SNAPSHOT")
public class OrchestratorVelocity {
    private final ProxyServer proxy; private final Logger log; private final Path dataDir;
    private OrchestratorEngine engine;

    @Inject public OrchestratorVelocity(ProxyServer proxy, Logger log, @DataDirectory Path dataDir){
        this.proxy=proxy; this.log=log; this.dataDir=dataDir;
    }

    @Subscribe public void onInit(ProxyInitializeEvent e){
        VelocityConfig C = new VelocityConfig(dataDir);
        log.info("MLP-Orchestrator (Velocity) starting...");

        ServerInventory inv = new VelocityInventory(proxy, C);
        PlayerOps pops = new VelocityPlayerOps(proxy, C);
        MetricsRepo repo = new MySQLRepo(C);
        ScalerOps scaler = new ScalerOps() {
            private int running = Math.max(1, proxy.getAllServers().size());
            @Override public int runningCount(){ return running; }
            @Override public void requestScaleTo(int n){
                running = n; if (C.logPlanOnly) log.info("[Scale] target instances -> {}", n);
            }
        };

        LstmClient lstm = new LstmClient(C.lstmBase, C.lstmTimeout);
        Regressor.Coef cf = new Regressor.Coef();
        cf.intercept=C.intercept; cf.yLag1=C.yLag1; cf.yLag2=C.yLag2; cf.beta=C.beta;
        Regressor reg = new Regressor(cf);

        Planner planner = new Planner(C.unitCostCpu, C.unitCostRamMb);
        planner.migratePenalty = C.migratePenalty; planner.allowOver = C.allowOver;
        planner.wCpu = C.scoreCpuW; planner.wRam = C.scoreRamW;

        Autoscaler autoscaler = new Autoscaler(C.cpuSteps, C.cooldownMin);
        autoscaler.setHysteresis(C.upH, C.downH);

        this.engine = new OrchestratorEngine(inv, pops, repo, scaler, lstm, reg, planner, autoscaler);

        proxy.getScheduler().buildTask(this, () -> {
            try { engine.runTick(); } catch (Throwable t){ t.printStackTrace(); }
        }).repeat(5, TimeUnit.MINUTES).schedule();

        log.info("MLP-Orchestrator (Velocity) ready.");
    }
}
